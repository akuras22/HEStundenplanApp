#include "MensaController.h"

#include "core/ImageNetworkCache.h"
#include "core/MensaRepository.h"
#include "core/SettingsStore.h"

#include <QLocale>

namespace stundenplan {

namespace {

QDate nearestWeekday(const QDate &date)
{
    if (date.dayOfWeek() == Qt::Saturday)
        return date.addDays(2);
    if (date.dayOfWeek() == Qt::Sunday)
        return date.addDays(1);
    return date;
}

QVariantMap locationToVariant(const MensaLocation &location)
{
    QVariantMap m;
    m[QStringLiteral("id")] = location.id;
    m[QStringLiteral("name")] = location.name;
    m[QStringLiteral("shortName")] = location.shortName;
    m[QStringLiteral("atHsEsslingen")] = location.atHsEsslingen;
    return m;
}

QVariantMap mealToVariant(const MensaMeal &meal)
{
    // The site always lists Studierende / Bedienstete / Gäste; anything else gets no made-up
    // group name.
    static const QStringList priceGroups = {
        QStringLiteral("Studierende"), QStringLiteral("Bedienstete"), QStringLiteral("Gäste")};
    QVariantList prices;
    for (qsizetype i = 0; i < meal.prices.size(); ++i) {
        QVariantMap p;
        p[QStringLiteral("group")] = meal.prices.size() == priceGroups.size()
            ? priceGroups.at(i)
            : QStringLiteral("Preis %1").arg(i + 1);
        p[QStringLiteral("price")] = meal.prices.at(i);
        prices.append(p);
    }

    QVariantList labels;
    for (const auto &label : meal.labels) {
        QVariantMap l;
        l[QStringLiteral("code")] = label.code;
        l[QStringLiteral("title")] = label.title;
        l[QStringLiteral("veggie")] = label.isVeggie();
        labels.append(l);
    }

    QVariantList nutrition;
    for (const auto &value : meal.nutrition) {
        QVariantMap n;
        n[QStringLiteral("label")] = value.label;
        n[QStringLiteral("value")] = value.value;
        n[QStringLiteral("sub")] = value.isSubItem;
        nutrition.append(n);
    }

    QVariantMap m;
    m[QStringLiteral("name")] = meal.name;
    m[QStringLiteral("category")] = meal.category;
    m[QStringLiteral("subcategory")] = meal.subcategory;
    m[QStringLiteral("imageUrl")] = meal.imageUrl;
    m[QStringLiteral("price")] = meal.prices.value(0);
    m[QStringLiteral("prices")] = prices;
    m[QStringLiteral("labels")] = labels;
    m[QStringLiteral("allergens")] = meal.allergens().join(QStringLiteral(", "));
    m[QStringLiteral("additives")] = meal.additives().join(QStringLiteral(", "));
    m[QStringLiteral("unknownCodes")] = meal.unknownCodes().join(QStringLiteral(", "));
    m[QStringLiteral("nutritionBasis")] = meal.nutritionBasis;
    m[QStringLiteral("nutrition")] = nutrition;
    return m;
}

} // namespace

MensaController::MensaController(SettingsStore *settings, QObject *parent)
    : QObject(parent)
    , m_settings(settings)
    , m_repository(new MensaRepository(this))
    , m_date(nearestWeekday(QDate::currentDate()))
{
    connect(m_settings, &SettingsStore::mensaLocationIdChanged, this, [this]() {
        Q_EMIT locationIdChanged();
        Q_EMIT dayChanged();
        loadCurrent();
    });
    connect(m_repository, &MensaRepository::dayFetched, this, [this](int locationId, const QDate &date, const MensaDay &day) {
        Entry &entry = m_entries[keyFor(locationId, date)];
        entry.day = day;
        entry.error.clear();
        entry.loading = false;
        if (locationId == this->locationId() && date == m_date)
            Q_EMIT dayChanged();
    });
    connect(m_repository, &MensaRepository::fetchFailed, this, [this](int locationId, const QDate &date, const QString &message) {
        Entry &entry = m_entries[keyFor(locationId, date)];
        entry.error = message;
        entry.loading = false;
        if (locationId == this->locationId() && date == m_date)
            Q_EMIT dayChanged();
    });
}

QString MensaController::keyFor(int locationId, const QDate &date)
{
    return QString::number(locationId) + QLatin1Char('|') + date.toString(Qt::ISODate);
}

QVariantList MensaController::locations() const
{
    QVariantList list;
    for (const auto &location : mensaLocations())
        list.append(locationToVariant(location));
    return list;
}

int MensaController::locationId() const
{
    return m_settings->mensaLocationId();
}

void MensaController::setLocationId(int id)
{
    // SettingsStore's change signal drives the rest (see the constructor).
    m_settings->setMensaLocationId(id);
}

QVariantMap MensaController::location() const
{
    if (auto location = mensaLocationById(locationId()))
        return locationToVariant(*location);
    return {};
}

void MensaController::setActive(bool active)
{
    if (m_active == active)
        return;
    m_active = active;
    Q_EMIT activeChanged();
    loadCurrent();
}

QString MensaController::dateTitle() const
{
    return QLocale(QLocale::German, QLocale::Germany).toString(m_date, QStringLiteral("dddd, d. MMMM yyyy"));
}

QVariantList MensaController::weekDays() const
{
    const QLocale german(QLocale::German, QLocale::Germany);
    const QDate monday = m_date.addDays(1 - m_date.dayOfWeek());
    QVariantList days;
    for (int i = 0; i < 5; ++i) {
        const QDate day = monday.addDays(i);
        QVariantMap m;
        m[QStringLiteral("date")] = day;
        m[QStringLiteral("dayLabel")] = german.dayName(day.dayOfWeek(), QLocale::ShortFormat).left(2);
        m[QStringLiteral("dateLabel")] = day.toString(QStringLiteral("dd.MM."));
        days.append(m);
    }
    return days;
}

const MensaController::Entry *MensaController::currentEntry() const
{
    const auto it = m_entries.constFind(keyFor(locationId(), m_date));
    return it == m_entries.constEnd() ? nullptr : &it.value();
}

QVariantList MensaController::categories() const
{
    QVariantList result;
    const Entry *entry = currentEntry();
    if (!entry || !entry->day)
        return result;
    QString currentName;
    QVariantList currentMeals;
    const auto flush = [&]() {
        if (currentMeals.isEmpty())
            return;
        QVariantMap category;
        category[QStringLiteral("name")] = currentName;
        category[QStringLiteral("meals")] = currentMeals;
        result.append(category);
        currentMeals.clear();
    };
    for (const auto &meal : entry->day->meals) {
        if (meal.category != currentName)
            flush();
        currentName = meal.category;
        currentMeals.append(mealToVariant(meal));
    }
    flush();
    return result;
}

bool MensaController::hasDay() const
{
    const Entry *entry = currentEntry();
    return entry && entry->day.has_value();
}

QString MensaController::closedMessage() const
{
    const Entry *entry = currentEntry();
    return entry && entry->day ? entry->day->closedMessage : QString();
}

bool MensaController::loading() const
{
    const Entry *entry = currentEntry();
    return entry && entry->loading;
}

QString MensaController::errorMessage() const
{
    const Entry *entry = currentEntry();
    return entry ? entry->error : QString();
}

void MensaController::setDate(const QDate &date)
{
    const QDate normalized = nearestWeekday(date);
    if (!normalized.isValid() || normalized == m_date)
        return;
    m_date = normalized;
    Q_EMIT dateChanged();
    Q_EMIT dayChanged();
    loadCurrent();
}

void MensaController::selectWeekday(int index)
{
    if (index < 0 || index > 4)
        return;
    setDate(m_date.addDays(index - dayIndex()));
}

void MensaController::stepDay(int delta)
{
    if (delta == 0)
        return;
    QDate date = m_date.addDays(delta);
    while (date.dayOfWeek() >= Qt::Saturday)
        date = date.addDays(delta > 0 ? 1 : -1);
    setDate(date);
}

void MensaController::goToday()
{
    setDate(QDate::currentDate());
}

void MensaController::refresh()
{
    loadCurrent(true);
}

void MensaController::clearCache()
{
    m_entries.clear();
    ImageNetworkCache::clear();
    Q_EMIT dayChanged();
    loadCurrent();
}

void MensaController::load(const QDate &date, bool force)
{
    const int id = locationId();
    if (id == 0)
        return;
    Entry &entry = m_entries[keyFor(id, date)];
    if (entry.loading || (entry.day && !force))
        return;
    entry.loading = true;
    m_repository->fetchDay(id, date);
}

void MensaController::loadCurrent(bool force)
{
    if (!m_active || locationId() == 0)
        return;
    load(m_date, force);
    Q_EMIT dayChanged();
    // The arrows' obvious next targets — fetched now, so stepping there is instant.
    QDate previous = m_date.addDays(-1);
    while (previous.dayOfWeek() >= Qt::Saturday)
        previous = previous.addDays(-1);
    QDate next = m_date.addDays(1);
    while (next.dayOfWeek() >= Qt::Saturday)
        next = next.addDays(1);
    load(previous, false);
    load(next, false);
}

} // namespace stundenplan

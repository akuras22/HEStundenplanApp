#include "SettingsStore.h"

#include <QStringList>

namespace stundenplan {

namespace {
const char *kConfigFile = "hestundenplanrc";
const char *kGroupStudiengang = "Studiengang";
const char *kGroupFavorites = "Favorites";
const char *kGroupHidden = "HiddenEvents";
const char *kGroupNotifications = "Notifications";
const char *kGroupAppearance = "Appearance";
const char *kFieldSep = "||";

QString encodeStudiengang(const Studiengang &s)
{
    return s.code + QLatin1String(kFieldSep) + s.abstgvnr + QLatin1String(kFieldSep) + s.parallelid;
}

std::optional<Studiengang> decodeStudiengang(const QString &raw)
{
    const auto parts = raw.split(QLatin1String(kFieldSep));
    if (parts.size() != 3)
        return std::nullopt;
    return Studiengang{parts[0], parts[1], parts[2]};
}

} // namespace

SettingsStore::SettingsStore(QObject *parent)
    : QObject(parent)
    , m_config(KSharedConfig::openConfig(QString::fromLatin1(kConfigFile)))
{
}

KConfigGroup SettingsStore::group(const QString &name) const
{
    return m_config->group(name);
}

std::optional<Studiengang> SettingsStore::selectedStudiengang() const
{
    auto g = group(QString::fromLatin1(kGroupStudiengang));
    const QString code = g.readEntry("code");
    const QString abstgvnr = g.readEntry("abstgvnr");
    const QString parallelid = g.readEntry("parallelid");
    if (code.isEmpty() || abstgvnr.isEmpty() || parallelid.isEmpty())
        return std::nullopt;
    return Studiengang{code, abstgvnr, parallelid};
}

void SettingsStore::setSelectedStudiengang(const Studiengang &studiengang)
{
    auto g = group(QString::fromLatin1(kGroupStudiengang));
    g.writeEntry("code", studiengang.code);
    g.writeEntry("abstgvnr", studiengang.abstgvnr);
    g.writeEntry("parallelid", studiengang.parallelid);
    g.sync();
    Q_EMIT selectedStudiengangChanged();
}

QList<Studiengang> SettingsStore::favoriteStudiengaenge() const
{
    auto g = group(QString::fromLatin1(kGroupFavorites));
    const auto encoded = g.readEntry("entries", QStringList());
    QList<Studiengang> result;
    for (const auto &raw : encoded) {
        if (auto s = decodeStudiengang(raw))
            result.append(*s);
    }
    std::sort(result.begin(), result.end(), [](const Studiengang &a, const Studiengang &b) { return a.code < b.code; });
    return result;
}

void SettingsStore::setFavorite(const Studiengang &studiengang, bool favorite)
{
    auto g = group(QString::fromLatin1(kGroupFavorites));
    auto encoded = g.readEntry("entries", QStringList());
    const QString key = encodeStudiengang(studiengang);
    encoded.removeAll(key);
    if (favorite)
        encoded.append(key);
    g.writeEntry("entries", encoded);
    g.sync();
    Q_EMIT favoritesChanged();
}

bool SettingsStore::isFavorite(const Studiengang &studiengang) const
{
    auto g = group(QString::fromLatin1(kGroupFavorites));
    return g.readEntry("entries", QStringList()).contains(encodeStudiengang(studiengang));
}

QSet<QString> SettingsStore::hiddenEventKeys() const
{
    auto g = group(QString::fromLatin1(kGroupHidden));
    const auto list = g.readEntry("keys", QStringList());
    return QSet<QString>(list.begin(), list.end());
}

void SettingsStore::setHidden(const QString &groupKey, bool hidden)
{
    auto g = group(QString::fromLatin1(kGroupHidden));
    auto list = g.readEntry("keys", QStringList());
    list.removeAll(groupKey);
    if (hidden)
        list.append(groupKey);
    g.writeEntry("keys", list);
    g.sync();
    Q_EMIT hiddenEventKeysChanged();
}

qint64 SettingsStore::lastUpdateCheckAt() const
{
    return group(QString::fromLatin1(kGroupNotifications)).readEntry("lastUpdateCheckAt", qint64(0));
}

void SettingsStore::setLastUpdateCheckAt(qint64 msecsSinceEpoch)
{
    auto g = group(QString::fromLatin1(kGroupNotifications));
    g.writeEntry("lastUpdateCheckAt", msecsSinceEpoch);
    g.sync();
}

bool SettingsStore::remindersEnabled() const
{
    return group(QString::fromLatin1(kGroupNotifications)).readEntry("remindersEnabled", false);
}

void SettingsStore::setRemindersEnabled(bool enabled)
{
    auto g = group(QString::fromLatin1(kGroupNotifications));
    g.writeEntry("remindersEnabled", enabled);
    g.sync();
    Q_EMIT remindersEnabledChanged();
}

bool SettingsStore::hasNotifiedToday(const QString &date, const QString &groupKey) const
{
    auto g = group(QString::fromLatin1(kGroupNotifications));
    if (g.readEntry("notifiedDate") != date)
        return false;
    return g.readEntry("notifiedKeys", QStringList()).contains(groupKey);
}

void SettingsStore::markNotifiedToday(const QString &date, const QString &groupKey)
{
    auto g = group(QString::fromLatin1(kGroupNotifications));
    const bool sameDay = g.readEntry("notifiedDate") == date;
    QStringList keys = sameDay ? g.readEntry("notifiedKeys", QStringList()) : QStringList();
    if (!keys.contains(groupKey))
        keys.append(groupKey);
    g.writeEntry("notifiedDate", date);
    g.writeEntry("notifiedKeys", keys);
    g.sync();
}

QSet<int> SettingsStore::reminderLeadMinutesSet() const
{
    auto g = group(QString::fromLatin1(kGroupNotifications));
    const auto raw = g.readEntry("reminderLeadMinutes", QStringList());
    if (raw.isEmpty())
        return {15};
    QSet<int> result;
    for (const auto &s : raw) {
        bool ok = false;
        const int v = s.toInt(&ok);
        if (ok)
            result.insert(v);
    }
    return result.isEmpty() ? QSet<int>{15} : result;
}

QVariantList SettingsStore::reminderLeadMinutes() const
{
    QVariantList result;
    for (int m : reminderLeadMinutesSet())
        result.append(m);
    return result;
}

void SettingsStore::setReminderLeadMinutes(const QVariantList &minutes)
{
    auto g = group(QString::fromLatin1(kGroupNotifications));
    QStringList raw;
    for (const auto &v : minutes)
        raw.append(QString::number(v.toInt()));
    g.writeEntry("reminderLeadMinutes", raw);
    g.sync();
}

bool SettingsStore::defaultViewIsDay() const
{
    return group(QString::fromLatin1(kGroupAppearance)).readEntry("defaultViewIsDay", false);
}

void SettingsStore::setDefaultViewIsDay(bool isDay)
{
    auto g = group(QString::fromLatin1(kGroupAppearance));
    g.writeEntry("defaultViewIsDay", isDay);
    g.sync();
    Q_EMIT defaultViewIsDayChanged();
}

bool SettingsStore::blockShowTime() const
{
    return group(QString::fromLatin1(kGroupAppearance)).readEntry("blockShowTime", true);
}

void SettingsStore::setBlockShowTime(bool show)
{
    auto g = group(QString::fromLatin1(kGroupAppearance));
    g.writeEntry("blockShowTime", show);
    g.sync();
    Q_EMIT blockShowTimeChanged();
}

bool SettingsStore::blockShowRoom() const
{
    return group(QString::fromLatin1(kGroupAppearance)).readEntry("blockShowRoom", true);
}

void SettingsStore::setBlockShowRoom(bool show)
{
    auto g = group(QString::fromLatin1(kGroupAppearance));
    g.writeEntry("blockShowRoom", show);
    g.sync();
    Q_EMIT blockShowRoomChanged();
}

bool SettingsStore::blockShowLecturer() const
{
    return group(QString::fromLatin1(kGroupAppearance)).readEntry("blockShowLecturer", false);
}

void SettingsStore::setBlockShowLecturer(bool show)
{
    auto g = group(QString::fromLatin1(kGroupAppearance));
    g.writeEntry("blockShowLecturer", show);
    g.sync();
    Q_EMIT blockShowLecturerChanged();
}

void SettingsStore::resetAppearance()
{
    auto g = group(QString::fromLatin1(kGroupAppearance));
    // Clean up the now-unused theme/accent keys from earlier versions, if present.
    g.deleteEntry("themeMode");
    g.deleteEntry("accentPreset");
    g.deleteEntry("customAccentColor");
    g.deleteEntry("customBackgroundColor");
    g.writeEntry("blockShowTime", true);
    g.writeEntry("blockShowRoom", true);
    g.writeEntry("blockShowLecturer", false);
    g.sync();
    Q_EMIT blockShowTimeChanged();
    Q_EMIT blockShowRoomChanged();
    Q_EMIT blockShowLecturerChanged();
}

} // namespace stundenplan

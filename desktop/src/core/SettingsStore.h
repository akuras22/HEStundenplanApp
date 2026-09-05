#pragma once

#include "Models.h"
#include <KConfigGroup>
#include <KSharedConfig>
#include <QColor>
#include <QObject>
#include <QSet>
#include <QVariantList>
#include <optional>

namespace stundenplan {

/**
 * Persists user settings via KConfig (~/.config/hestundenplanrc) — the chosen/favorite
 * Studiengänge, hidden recurring event groups, appearance and notification preferences. Never any
 * timetable data itself (see TimetableCache for that). Ported from SettingsStore.kt.
 */
class SettingsStore : public QObject
{
    Q_OBJECT
    Q_PROPERTY(ThemeMode themeMode READ themeMode WRITE setThemeMode NOTIFY themeModeChanged)
    Q_PROPERTY(AccentPreset accentPreset READ accentPreset WRITE setAccentPreset NOTIFY accentPresetChanged)
    Q_PROPERTY(QColor customAccentColor READ customAccentColor WRITE setCustomAccentColor NOTIFY customAccentColorChanged)
    Q_PROPERTY(QColor customBackgroundColor READ customBackgroundColor WRITE setCustomBackgroundColor NOTIFY
                   customBackgroundColorChanged)
    Q_PROPERTY(bool defaultViewIsDay READ defaultViewIsDay WRITE setDefaultViewIsDay NOTIFY defaultViewIsDayChanged)
    Q_PROPERTY(bool blockShowTime READ blockShowTime WRITE setBlockShowTime NOTIFY blockShowTimeChanged)
    Q_PROPERTY(bool blockShowRoom READ blockShowRoom WRITE setBlockShowRoom NOTIFY blockShowRoomChanged)
    Q_PROPERTY(bool blockShowLecturer READ blockShowLecturer WRITE setBlockShowLecturer NOTIFY blockShowLecturerChanged)
    Q_PROPERTY(bool remindersEnabled READ remindersEnabled WRITE setRemindersEnabled NOTIFY remindersEnabledChanged)

public:
    enum class ThemeMode { System, Light, Dark };
    Q_ENUM(ThemeMode)

    enum class AccentPreset { Default, Green, Purple, Orange, Red, Pink, Custom };
    Q_ENUM(AccentPreset)

    static QColor accentPresetColor(AccentPreset preset);
    static QString accentPresetLabel(AccentPreset preset);

    explicit SettingsStore(QObject *parent = nullptr);

    std::optional<Studiengang> selectedStudiengang() const;
    Q_INVOKABLE void setSelectedStudiengang(const Studiengang &studiengang);

    QList<Studiengang> favoriteStudiengaenge() const;
    Q_INVOKABLE void setFavorite(const Studiengang &studiengang, bool favorite);
    Q_INVOKABLE bool isFavorite(const Studiengang &studiengang) const;

    QSet<QString> hiddenEventKeys() const;
    Q_INVOKABLE void setHidden(const QString &groupKey, bool hidden);

    qint64 lastUpdateCheckAt() const;
    void setLastUpdateCheckAt(qint64 msecsSinceEpoch);

    bool remindersEnabled() const;
    void setRemindersEnabled(bool enabled);

    bool hasNotifiedToday(const QString &date, const QString &groupKey) const;
    void markNotifiedToday(const QString &date, const QString &groupKey);

    QSet<int> reminderLeadMinutesSet() const;
    Q_INVOKABLE QVariantList reminderLeadMinutes() const;
    Q_INVOKABLE void setReminderLeadMinutes(const QVariantList &minutes);

    ThemeMode themeMode() const;
    void setThemeMode(ThemeMode mode);

    AccentPreset accentPreset() const;
    void setAccentPreset(AccentPreset preset);

    QColor customAccentColor() const;
    void setCustomAccentColor(const QColor &color);

    QColor customBackgroundColor() const;
    void setCustomBackgroundColor(const QColor &color);

    bool defaultViewIsDay() const;
    void setDefaultViewIsDay(bool isDay);

    bool blockShowTime() const;
    void setBlockShowTime(bool show);
    bool blockShowRoom() const;
    void setBlockShowRoom(bool show);
    bool blockShowLecturer() const;
    void setBlockShowLecturer(bool show);

    Q_INVOKABLE void resetAppearance();

Q_SIGNALS:
    void themeModeChanged();
    void accentPresetChanged();
    void customAccentColorChanged();
    void customBackgroundColorChanged();
    void defaultViewIsDayChanged();
    void blockShowTimeChanged();
    void blockShowRoomChanged();
    void blockShowLecturerChanged();
    void remindersEnabledChanged();
    void favoritesChanged();
    void hiddenEventKeysChanged();
    void selectedStudiengangChanged();

private:
    KConfigGroup group(const QString &name) const;
    KSharedConfigPtr m_config;
};

} // namespace stundenplan

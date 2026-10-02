#pragma once

#include "MensaModels.h"
#include <QDate>
#include <QNetworkAccessManager>
#include <QObject>

namespace stundenplan {

/**
 * Fetches the Studierendenwerk Stuttgart's Speiseplan live. Their page
 * (studierendenwerk-stuttgart.de/essen/speiseplan) only embeds an iframe from sws2.maxmanager.xyz,
 * which in turn loads each day via the same POST this makes — so this asks exactly what the
 * website itself asks, one location and one day at a time. Ported from MensaRepository.kt.
 */
class MensaRepository : public QObject
{
    Q_OBJECT
public:
    explicit MensaRepository(QObject *parent = nullptr);

    void fetchDay(int locationId, const QDate &date);

Q_SIGNALS:
    void dayFetched(int locationId, const QDate &date, const stundenplan::MensaDay &day);
    void fetchFailed(int locationId, const QDate &date, const QString &message);

private:
    QNetworkAccessManager *m_manager;
};

} // namespace stundenplan

#pragma once

#include "TransitParser.h"
#include <QDateTime>
#include <QNetworkAccessManager>
#include <QObject>
#include <QPointer>
#include <QNetworkReply>
#include <functional>

namespace stundenplan {

/**
 * Live departures and stop search from the VVS (Verkehrs- und Tarifverbund Stuttgart) — the EFA
 * backend behind vvs.de's own departure monitor, covering Esslingen's city buses, the S-Bahn,
 * regional trains and Göppingen. Ported from TransitRepository.kt.
 */
class TransitRepository : public QObject
{
    Q_OBJECT
public:
    explicit TransitRepository(QObject *parent = nullptr);

    /** The next departures from `stopId` — from now, or from `at` when it's valid. A newer call
     *  supersedes (aborts) one still in flight. */
    void fetchDepartures(const QString &stopId,
                         const QDateTime &at,
                         const std::function<void(const QList<Departure> &)> &onSuccess,
                         const std::function<void(const QString &)> &onError);

    void searchStops(const QString &query,
                     const std::function<void(const QList<TransitStop> &)> &onSuccess,
                     const std::function<void(const QString &)> &onError);

private:
    QNetworkReply *get(const QUrl &url,
                       const std::function<void(const QByteArray &)> &onSuccess,
                       const std::function<void(const QString &)> &onError);

    QNetworkAccessManager *m_manager;
    QPointer<QNetworkReply> m_pendingDepartures;
    QPointer<QNetworkReply> m_pendingSearch;
};

} // namespace stundenplan

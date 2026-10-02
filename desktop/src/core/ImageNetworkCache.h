#pragma once

#include <QQmlNetworkAccessManagerFactory>

namespace stundenplan {

/**
 * Gives the QML engine's network access (only ever used for remote Image sources — the
 * Speiseplan photos) a disk cache under ~/.cache, so revisiting a day doesn't redownload its
 * ~1 MB photos. The photo server sends no Cache-Control/Expires headers, and its URLs are already
 * versioned ("?v=1"), so cached copies are used without asking the server again.
 */
class ImageNetworkCache : public QQmlNetworkAccessManagerFactory
{
public:
    QNetworkAccessManager *create(QObject *parent) override;

    /** Empties the disk cache of every network access manager created so far
     *  ("Zwischenspeicher leeren"). */
    static void clear();
};

} // namespace stundenplan

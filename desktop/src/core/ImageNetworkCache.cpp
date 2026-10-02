#include "ImageNetworkCache.h"

#include <QDir>
#include <QList>
#include <QMutex>
#include <QNetworkAccessManager>
#include <QNetworkDiskCache>
#include <QNetworkRequest>
#include <QPointer>
#include <QStandardPaths>

namespace stundenplan {

namespace {

constexpr qint64 kMaxCacheBytes = 200LL * 1024 * 1024;

class CacheFirstNetworkAccessManager : public QNetworkAccessManager
{
public:
    using QNetworkAccessManager::QNetworkAccessManager;

protected:
    QNetworkReply *createRequest(Operation op, const QNetworkRequest &originalRequest, QIODevice *outgoingData) override
    {
        QNetworkRequest request(originalRequest);
        request.setAttribute(QNetworkRequest::CacheLoadControlAttribute, QNetworkRequest::PreferCache);
        return QNetworkAccessManager::createRequest(op, request, outgoingData);
    }
};

// The engine creates one manager per thread that loads images, so there can be several caches
// (all sharing one directory) living on different threads.
QMutex s_cachesMutex;
QList<QPointer<QNetworkDiskCache>> s_caches;

QString cacheDirectory()
{
    return QStandardPaths::writableLocation(QStandardPaths::CacheLocation) + QStringLiteral("/images");
}

} // namespace

QNetworkAccessManager *ImageNetworkCache::create(QObject *parent)
{
    auto *manager = new CacheFirstNetworkAccessManager(parent);
    auto *cache = new QNetworkDiskCache(manager);
    cache->setCacheDirectory(cacheDirectory());
    cache->setMaximumCacheSize(kMaxCacheBytes);
    manager->setCache(cache);

    QMutexLocker lock(&s_cachesMutex);
    s_caches.append(cache);
    return manager;
}

void ImageNetworkCache::clear()
{
    QMutexLocker lock(&s_cachesMutex);
    bool anyLive = false;
    for (const auto &cache : std::as_const(s_caches)) {
        if (!cache)
            continue;
        // Each cache belongs to the thread that loads images through it; clear() scans the
        // whole (shared) directory, so any one of them empties it for all.
        QMetaObject::invokeMethod(cache, &QNetworkDiskCache::clear, Qt::QueuedConnection);
        anyLive = true;
    }
    // No image loaded yet this session: nothing holds the directory open, so just delete it.
    if (!anyLive)
        QDir(cacheDirectory()).removeRecursively();
}

} // namespace stundenplan

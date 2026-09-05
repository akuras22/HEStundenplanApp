#pragma once

#include <QList>
#include <QString>

extern "C" {
struct _xmlDoc;
struct _xmlNode;
}

namespace stundenplan {

/**
 * Minimal libxml2-backed HTML DOM wrapper, providing just the subset of Jsoup's API that
 * QisParser.kt relies on (select by tag/class, direct-child selection, text(), attr(),
 * className(), closest()). This lets the C++ port of QisParser stay structurally close to the
 * original Kotlin implementation.
 */
class HtmlElement
{
public:
    HtmlElement() = default;
    explicit HtmlElement(_xmlNode *node) : m_node(node) { }

    bool isValid() const { return m_node != nullptr; }
    explicit operator bool() const { return isValid(); }

    QString tagName() const;
    QString attr(const QString &name) const;
    QString className() const { return attr(QStringLiteral("class")); }

    /** Concatenated, whitespace-collapsed text content of this element and all descendants. */
    QString text() const;

    /** Direct element children (no text/comment nodes), in document order. */
    QList<HtmlElement> children() const;

    /** All descendant elements with the given (lowercase) tag name, depth-first. */
    QList<HtmlElement> selectDescendants(const QString &tag) const;

    /** All descendant elements with the given tag name and an exact class token match. */
    QList<HtmlElement> selectDescendantsByClass(const QString &tag, const QString &className) const;

    /** Direct child elements with the given tag name. */
    QList<HtmlElement> selectDirectChildren(const QString &tag) const;

    HtmlElement firstDescendant(const QString &tag) const;

    /** Walks up parents (including self) to the nearest ancestor with the given tag name. */
    HtmlElement closest(const QString &tag) const;

    _xmlNode *node() const { return m_node; }

private:
    _xmlNode *m_node = nullptr;
};

class HtmlDocument
{
public:
    explicit HtmlDocument(const QString &html);
    ~HtmlDocument();

    HtmlDocument(const HtmlDocument &) = delete;
    HtmlDocument &operator=(const HtmlDocument &) = delete;

    bool isValid() const { return m_doc != nullptr; }
    HtmlElement root() const;

private:
    _xmlDoc *m_doc = nullptr;
};

} // namespace stundenplan

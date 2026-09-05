#include "HtmlDocument.h"

#include <QRegularExpression>
#include <libxml/HTMLparser.h>
#include <libxml/tree.h>

namespace stundenplan {

namespace {

bool isElement(const xmlNode *node)
{
    return node && node->type == XML_ELEMENT_NODE;
}

QString nodeTagName(const xmlNode *node)
{
    if (!node || !node->name)
        return {};
    return QString::fromUtf8(reinterpret_cast<const char *>(node->name)).toLower();
}

void collectText(const xmlNode *node, QString &out)
{
    for (const xmlNode *child = node->children; child; child = child->next) {
        if (child->type == XML_TEXT_NODE || child->type == XML_CDATA_SECTION_NODE) {
            if (child->content) {
                out += QLatin1Char(' ');
                out += QString::fromUtf8(reinterpret_cast<const char *>(child->content));
            }
        } else if (child->type == XML_ELEMENT_NODE) {
            collectText(child, out);
        }
    }
}

void collectDescendantsByTag(const xmlNode *node, const QString &tag, QList<HtmlElement> &out)
{
    for (xmlNode *child = node->children; child; child = child->next) {
        if (!isElement(child))
            continue;
        if (nodeTagName(child) == tag)
            out.append(HtmlElement(child));
        collectDescendantsByTag(child, tag, out);
    }
}

} // namespace

QString HtmlElement::tagName() const
{
    return nodeTagName(m_node);
}

QString HtmlElement::attr(const QString &name) const
{
    if (!m_node)
        return {};
    xmlChar *value = xmlGetProp(m_node, reinterpret_cast<const xmlChar *>(name.toUtf8().constData()));
    if (!value)
        return {};
    QString result = QString::fromUtf8(reinterpret_cast<const char *>(value));
    xmlFree(value);
    return result;
}

QString HtmlElement::text() const
{
    if (!m_node)
        return {};
    QString out;
    collectText(m_node, out);
    // Collapse all whitespace runs (spaces, tabs, newlines) into single spaces, then trim —
    // matches Jsoup's Element.text() normalization closely enough for this table-layout HTML.
    static const QRegularExpression whitespaceRun(QStringLiteral("\\s+"));
    return out.replace(whitespaceRun, QStringLiteral(" ")).trimmed();
}

QList<HtmlElement> HtmlElement::children() const
{
    QList<HtmlElement> result;
    if (!m_node)
        return result;
    for (xmlNode *child = m_node->children; child; child = child->next) {
        if (isElement(child))
            result.append(HtmlElement(child));
    }
    return result;
}

QList<HtmlElement> HtmlElement::selectDescendants(const QString &tag) const
{
    QList<HtmlElement> result;
    if (!m_node)
        return result;
    collectDescendantsByTag(m_node, tag.toLower(), result);
    return result;
}

QList<HtmlElement> HtmlElement::selectDescendantsByClass(const QString &tag, const QString &wantedClass) const
{
    QList<HtmlElement> result;
    for (const auto &el : selectDescendants(tag)) {
        const QString classes = el.className();
        for (const auto &token : classes.split(QLatin1Char(' '), Qt::SkipEmptyParts)) {
            if (token == wantedClass) {
                result.append(el);
                break;
            }
        }
    }
    return result;
}

QList<HtmlElement> HtmlElement::selectDirectChildren(const QString &tag) const
{
    QList<HtmlElement> result;
    const QString wanted = tag.toLower();
    for (const auto &child : children()) {
        if (child.tagName() == wanted)
            result.append(child);
    }
    return result;
}

HtmlElement HtmlElement::firstDescendant(const QString &tag) const
{
    auto all = selectDescendants(tag);
    return all.isEmpty() ? HtmlElement() : all.first();
}

HtmlElement HtmlElement::closest(const QString &tag) const
{
    const QString wanted = tag.toLower();
    xmlNode *cur = m_node;
    while (cur) {
        if (isElement(cur) && nodeTagName(cur) == wanted)
            return HtmlElement(cur);
        cur = cur->parent;
    }
    return HtmlElement();
}

HtmlDocument::HtmlDocument(const QString &html)
{
    const QByteArray utf8 = html.toUtf8();
    m_doc = htmlReadMemory(utf8.constData(),
                            utf8.size(),
                            nullptr,
                            "UTF-8",
                            HTML_PARSE_RECOVER | HTML_PARSE_NOERROR | HTML_PARSE_NOWARNING | HTML_PARSE_NONET);
}

HtmlDocument::~HtmlDocument()
{
    if (m_doc)
        xmlFreeDoc(m_doc);
}

HtmlElement HtmlDocument::root() const
{
    if (!m_doc)
        return HtmlElement();
    return HtmlElement(xmlDocGetRootElement(m_doc));
}

} // namespace stundenplan

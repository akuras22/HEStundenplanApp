#pragma once

#include "MensaModels.h"
#include <QString>

namespace stundenplan {

/**
 * Parses one day's menu as returned by the Studierendenwerk's Speiseplan backend (the HTML
 * fragment its page injects into #speiseplan). The fragment is a flat run of Bootstrap rows in
 * display order: a "gruppenkopf" row per category, optional "untergruppenkopf" rows below it,
 * then one "splMeal" row per dish — or a single "nodata" row on closed days. Ported from
 * MensaParser.kt.
 */
namespace MensaParser {

inline constexpr const char *kBaseUrl = "https://sws2.maxmanager.xyz/";

MensaDay parseDay(const QString &html);

} // namespace MensaParser

} // namespace stundenplan

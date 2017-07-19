package de.ph1b.audiobook.features.chapterReader.matroska

data class MatroskaChapter(
    val startTime: Long,
    val names: List<MatroskaChapterName>,
    val children: List<MatroskaChapter>) {
  fun getName(vararg preferredLanguages: String): String?
      = preferredLanguages
      .map { language ->
        names.find { language in it.languages }?.name
      }
      .filterNotNull()
      .firstOrNull() ?: names.firstOrNull()?.name
}
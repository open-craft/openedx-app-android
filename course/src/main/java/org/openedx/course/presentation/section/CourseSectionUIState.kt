package org.openedx.course.presentation.section

import org.openedx.core.domain.model.Block
import org.openedx.core.module.db.DownloadedState

sealed class CourseSectionUIState {
    data class Blocks(
        val blocks: List<Block>,
        val downloadedState: Map<String, DownloadedState>,
        val sectionName: String,
        val courseName: String
    ) : CourseSectionUIState()
    data class Gated(
        val gatedSubsectionName: String?,
        val prereqSubsectionName: String?,
        val prereqId: String?,
    ) : CourseSectionUIState()
    object Loading : CourseSectionUIState()
}

package org.openedx.course.domain.interactor

import org.openedx.core.BlockType
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseStructure
import org.openedx.course.data.repository.CourseRepository

class CourseInteractor(
    private val repository: CourseRepository
) {

    suspend fun preloadCourseStructure(courseId: String) =
        repository.preloadCourseStructure(courseId)

    suspend fun preloadCourseStructureFromCache(courseId: String) =
        repository.preloadCourseStructureFromCache(courseId)

    @Throws(IllegalStateException::class)
    fun getCourseStructureFromCache() = repository.getCourseStructureFromCache()

    @Throws(IllegalStateException::class)
    fun getCourseStructureForVideos(): CourseStructure {
        val courseStructure = repository.getCourseStructureFromCache()
        val blocks = courseStructure.blockData
        val videoBlocks = blocks.filter { it.type == BlockType.VIDEO }
        val resultBlocks = ArrayList<Block>()
        videoBlocks.forEach { videoBlock ->
            val verticalBlock = blocks.firstOrNull { it.descendants.contains(videoBlock.id) }
            if (verticalBlock != null) {
                val sequentialBlock =
                    blocks.firstOrNull { it.descendants.contains(verticalBlock.id) }
                if (sequentialBlock != null) {
                    val chapterBlock =
                        blocks.firstOrNull { it.descendants.contains(sequentialBlock.id) }
                    if (chapterBlock != null) {
                        resultBlocks.add(videoBlock)
                        val verticalIndex = resultBlocks.indexOfFirst { it.id == verticalBlock.id }
                        if (verticalIndex == -1) {
                            resultBlocks.add(verticalBlock.copy(descendants = listOf(videoBlock.id)))
                        } else {
                            val block = resultBlocks[verticalIndex]
                            resultBlocks[verticalIndex] =
                                block.copy(descendants = block.descendants + videoBlock.id)
                        }
                        if (!resultBlocks.contains(sequentialBlock)) {
                            resultBlocks.add(sequentialBlock)
                        }
                        if (!resultBlocks.contains(chapterBlock)) {
                            resultBlocks.add(chapterBlock)
                        }
                    }
                }

            }
        }
        return courseStructure.copy(blockData = resultBlocks.toList())
    }

    suspend fun getCourseStatus(courseId: String) = repository.getCourseStatus(courseId)

    suspend fun getCourseDates(courseId: String) = repository.getCourseDates(courseId)

    suspend fun resetCourseDates(courseId: String) = repository.resetCourseDates(courseId)

    suspend fun getDatesBannerInfo(courseId: String) = repository.getDatesBannerInfo(courseId)

    suspend fun getHandouts(courseId: String) = repository.getHandouts(courseId)

    suspend fun getAnnouncements(courseId: String) = repository.getAnnouncements(courseId)

    suspend fun removeDownloadModel(id: String) = repository.removeDownloadModel(id)

    suspend fun getSubsection(subsectionId: String) = repository.getSequence(subsectionId)

    fun getDownloadModels() = repository.getDownloadModels()

}

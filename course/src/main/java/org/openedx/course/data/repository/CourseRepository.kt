package org.openedx.course.data.repository

import kotlinx.coroutines.flow.map
import org.openedx.core.ApiConstants
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.BlocksCompletionBody
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.*
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.module.db.DownloadDao
import org.openedx.course.data.storage.CourseDao

class CourseRepository(
    private val api: CourseApi,
    private val courseDao: CourseDao,
    private val downloadDao: DownloadDao,
    private val preferencesManager: CorePreferences,
) {
    private var courseStructure: CourseStructure? = null

    suspend fun removeDownloadModel(id: String) {
        downloadDao.removeDownloadModel(id)
    }

    fun getDownloadModels() = downloadDao.readAllData().map { list ->
        list.map { it.mapToDomain() }
    }

    suspend fun preloadCourseStructure(courseId: String) {
        val response = api.getCourseStructure(
            "stale-if-error=0",
            "v3",
            preferencesManager.user?.username,
            courseId
        )
        courseDao.insertCourseStructureEntity(response.mapToRoomEntity())
        courseStructure = null
        courseStructure = response.mapToDomain()
    }

    suspend fun preloadCourseStructureFromCache(courseId: String) {
        val cachedCourseStructure = courseDao.getCourseStructureById(courseId)
        courseStructure = null
        if (cachedCourseStructure != null) {
            courseStructure = cachedCourseStructure.mapToDomain()
        } else {
            throw NoCachedDataException()
        }
    }

    @Throws(IllegalStateException::class)
    fun getCourseStructureFromCache(): CourseStructure {
        if (courseStructure != null) {
            return courseStructure!!
        } else {
            throw IllegalStateException("Course structure is empty")
        }
    }

    suspend fun getCourseStatus(courseId: String): CourseComponentStatus {
        val username = preferencesManager.user?.username ?: ""
        return api.getCourseStatus(username, courseId).mapToDomain()
    }

    suspend fun markBlocksCompletion(courseId: String, blocksId: List<String>) {
        val username = preferencesManager.user?.username ?: ""
        val blocksCompletionBody = BlocksCompletionBody(
            username,
            courseId,
            blocksId.associateWith { "1" }.toMap()
        )
        return api.markBlocksCompletion(blocksCompletionBody)
    }

    suspend fun getCourseDates(courseId: String) =
        api.getCourseDates(courseId).getCourseDatesResult()

    suspend fun resetCourseDates(courseId: String) =
        api.resetCourseDates(mapOf(ApiConstants.COURSE_KEY to courseId)).mapToDomain()

    suspend fun getDatesBannerInfo(courseId: String) =
        api.getDatesBannerInfo(courseId).mapToDomain()

    suspend fun getHandouts(courseId: String) = api.getHandouts(courseId).mapToDomain()

    suspend fun getAnnouncements(courseId: String) =
        api.getAnnouncements(courseId).map { it.mapToDomain() }

    suspend fun getSequence(sectionId: String) = api.getSequence(sectionId).mapToDomain()
}

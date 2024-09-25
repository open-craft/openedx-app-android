package org.openedx.dashboard.domain.interactor

import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.dashboard.data.repository.DashboardRepository
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.data.repository.DiscoveryRepository

class DashboardInteractor(
    private val dashboardRepository: DashboardRepository,
    private val discoveryRepository: DiscoveryRepository
) {

    suspend fun getEnrolledCourses(page: Int): DashboardCourseList {
        return dashboardRepository.getEnrolledCourses(page)
    }

    suspend fun getEnrolledCoursesFromCache() = dashboardRepository.getEnrolledCoursesFromCache()

    suspend fun getCourseDetails(courseId: String): Course {
        return discoveryRepository.getCourseDetail(courseId)
    }

    suspend fun getCourseDetailsFromCache(courseId: String): Course? {
        return discoveryRepository.getCourseDetailFromCache(courseId)
    }
}

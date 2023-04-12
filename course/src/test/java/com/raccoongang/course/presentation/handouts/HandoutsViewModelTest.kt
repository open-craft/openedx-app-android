package com.raccoongang.course.presentation.handouts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.core.domain.model.*
import com.raccoongang.course.domain.interactor.CourseInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class HandoutsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val interactor = mockk<CourseInteractor>()

    //region mockHandoutsModel

    private val handoutsModel = HandoutsModel("")

    //endregion

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getEnrolledCourse no internet connection exception`() = runTest {
        val viewModel = HandoutsViewModel("", "Handouts", interactor)
        coEvery { interactor.getHandouts(any()) } throws UnknownHostException()

        advanceUntilIdle()

        assert(viewModel.htmlContent.value == null)
    }

    @Test
    fun `getEnrolledCourse unknown exception`() = runTest {
        val viewModel = HandoutsViewModel("", "Handouts", interactor)
        coEvery { interactor.getHandouts(any()) } throws Exception()
        advanceUntilIdle()

        assert(viewModel.htmlContent.value == null)
    }

    @Test
    fun `getEnrolledCourse handouts success`() = runTest {
        val viewModel = HandoutsViewModel("", HandoutsType.Handouts.name, interactor)
        coEvery { interactor.getHandouts(any()) } returns HandoutsModel("hello")

        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getHandouts(any()) }
        coVerify(exactly = 0) { interactor.getAnnouncements(any()) }

        assert(viewModel.htmlContent.value != null)
    }

    @Test
    fun `getEnrolledCourse announcements success`() = runTest {
        val viewModel = HandoutsViewModel("", HandoutsType.Announcements.name, interactor)
        coEvery { interactor.getAnnouncements(any()) } returns listOf(
            AnnouncementModel(
                "date",
                "content"
            )
        )

        advanceUntilIdle()
        coVerify(exactly = 0) { interactor.getHandouts(any()) }
        coVerify(exactly = 1) { interactor.getAnnouncements(any()) }

        assert(viewModel.htmlContent.value != null)
    }

    @Test
    fun `injectDarkMode test`() = runTest {
        val viewModel = HandoutsViewModel("", HandoutsType.Announcements.name, interactor)
        coEvery { interactor.getAnnouncements(any()) } returns listOf(
            AnnouncementModel(
                "date",
                "content"
            )
        )
        viewModel.injectDarkMode(
            viewModel.htmlContent.value.toString(),
            ULong.MAX_VALUE,
            ULong.MAX_VALUE
        )
        advanceUntilIdle()
        coVerify(exactly = 0) { interactor.getHandouts(any()) }
        coVerify(exactly = 1) { interactor.getAnnouncements(any()) }

        assert(viewModel.htmlContent.value != null)
    }

}
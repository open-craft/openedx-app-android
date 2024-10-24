package org.openedx.course.presentation.section

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.BlockType
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.extension.serializable
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.R
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.ui.CardArrow
import java.io.File

class CourseSectionFragment : Fragment() {

    private val viewModel by viewModel<CourseSectionViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        val subSectionId = requireArguments().getString(ARG_SUBSECTION_ID, "")
        viewModel.mode = requireArguments().serializable(ARG_MODE)!!
        viewModel.getBlocks(subSectionId, viewModel.mode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(CourseSectionUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                CourseSectionScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onItemClick = { block ->
                        if (block.descendants.isNotEmpty()) {
                            viewModel.verticalClickedEvent(block.blockId)
                            router.navigateToCourseContainer(
                                fm = requireActivity().supportFragmentManager,
                                courseId = viewModel.courseId,
                                unitId = block.id,
                                mode = viewModel.mode
                            )
                        }
                    },
                    onDownloadClick = {
                        if (viewModel.isBlockDownloading(it.id) || viewModel.isBlockDownloaded(it.id)) {
                            viewModel.removeDownloadModels(it.id)
                        } else {
                            viewModel.saveDownloadModels(
                                requireContext().externalCacheDir.toString() +
                                        File.separator +
                                        requireContext()
                                            .getString(org.openedx.core.R.string.app_name)
                                            .replace(Regex("\\s"), "_"), it.id
                            )
                        }
                    },
                    onGoToPrerequisiteClick = { subSectionId ->
                        viewModel.goToPrerequisiteSectionClickedEvent(subSectionId)
                        router.navigateToCourseSubsections(
                            fm = requireActivity().supportFragmentManager,
                            courseId = viewModel.courseId,
                            subSectionId = subSectionId,
                            mode = CourseViewMode.FULL
                        )
                    }
                )

                LaunchedEffect(rememberSaveable { true }) {
                    val unitId = requireArguments().getString(ARG_UNIT_ID, "")
                    if (unitId.isNotEmpty()) {
                        router.navigateToCourseContainer(
                            fm = requireActivity().supportFragmentManager,
                            courseId = viewModel.courseId,
                            unitId = unitId,
                            componentId = requireArguments().getString(ARG_COMPONENT_ID, ""),
                            mode = viewModel.mode
                        )
                        requireArguments().putString(ARG_UNIT_ID, "")
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_SUBSECTION_ID = "subSectionId"
        private const val ARG_UNIT_ID = "unitId"
        private const val ARG_COMPONENT_ID = "componentId"
        private const val ARG_MODE = "mode"
        fun newInstance(
            courseId: String,
            subSectionId: String,
            unitId: String?,
            componentId: String?,
            mode: CourseViewMode,
        ): CourseSectionFragment {
            val fragment = CourseSectionFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_SUBSECTION_ID to subSectionId,
                ARG_UNIT_ID to unitId,
                ARG_COMPONENT_ID to componentId,
                ARG_MODE to mode
            )
            return fragment
        }
    }
}

@Composable
private fun CourseSectionScreen(
    windowSize: WindowSize,
    uiState: CourseSectionUIState,
    uiMessage: UIMessage?,
    onBackClick: () -> Unit,
    onItemClick: (Block) -> Unit,
    onDownloadClick: (Block) -> Unit,
    onGoToPrerequisiteClick: (String) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val title = when (uiState) {
        is CourseSectionUIState.Blocks -> uiState.sectionName
        else -> ""
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val listPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(vertical = 24.dp),
                    compact = PaddingValues(24.dp)
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsInset(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(contentWidth) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .displayCutoutForLandscape()
                        .zIndex(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BackBtn {
                        onBackClick()
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        text = title,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape
                ) {
                    when (uiState) {
                        is CourseSectionUIState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is CourseSectionUIState.Gated -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_course_gated),
                                    contentDescription = "gated",
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxWidth(),
                                    text = stringResource(
                                        id = R.string.course_gated_subsection,
                                        uiState.prereqSubsectionName ?: ""
                                    ),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.appTypography.titleMedium,
                                    color = MaterialTheme.appColors.textPrimary,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OpenEdXButton(
                                    text = stringResource(id = R.string.course_go_to_prerequisite_section),
                                    onClick = {
                                        onGoToPrerequisiteClick(uiState.prereqId ?: "")
                                    },
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }

                        is CourseSectionUIState.Blocks -> {
                            Column(Modifier.fillMaxSize()) {
                                LazyColumn(
                                    contentPadding = listPadding,
                                ) {
                                    items(uiState.blocks) { block ->
                                        CourseSubsectionItem(
                                            block = block,
                                            downloadedState = uiState.downloadedState[block.id],
                                            onClick = {
                                                onItemClick(it)
                                            },
                                            onDownloadClick = onDownloadClick
                                        )
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseSubsectionItem(
    block: Block,
    downloadedState: DownloadedState?,
    onClick: (Block) -> Unit,
    onDownloadClick: (Block) -> Unit
) {
    val completedIconPainter =
        if (block.isCompleted()) painterResource(R.drawable.course_ic_task_alt) else painterResource(
            R.drawable.ic_course_chapter_icon
        )
    val completedIconColor =
        if (block.isCompleted()) MaterialTheme.appColors.primary else MaterialTheme.appColors.onSurface
    val completedIconDescription = if (block.isCompleted()) {
        stringResource(id = R.string.course_accessibility_section_completed)
    } else {
        stringResource(id = R.string.course_accessibility_section_uncompleted)
    }

    val iconModifier = Modifier.size(24.dp)

    Column(Modifier.clickable { onClick(block) }) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(
                    horizontal = 20.dp,
                    vertical = 24.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = completedIconPainter,
                contentDescription = completedIconDescription,
                tint = completedIconColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = block.displayName,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (downloadedState == DownloadedState.DOWNLOADED || downloadedState == DownloadedState.NOT_DOWNLOADED) {
                    val downloadIconPainter = if (downloadedState == DownloadedState.DOWNLOADED) {
                        painterResource(id = R.drawable.course_ic_remove_download)
                    } else {
                        painterResource(id = R.drawable.course_ic_start_download)
                    }
                    val downloadIconDescription =
                        if (downloadedState == DownloadedState.DOWNLOADED) {
                            stringResource(id = R.string.course_accessibility_remove_course_section)
                        } else {
                            stringResource(id = R.string.course_accessibility_download_course_section)
                        }
                    IconButton(modifier = iconModifier,
                        onClick = { onDownloadClick(block) }) {
                        Icon(
                            painter = downloadIconPainter,
                            contentDescription = downloadIconDescription,
                            tint = MaterialTheme.appColors.textPrimary
                        )
                    }
                } else if (downloadedState != null) {
                    Box(contentAlignment = Alignment.Center) {
                        if (downloadedState == DownloadedState.DOWNLOADING || downloadedState == DownloadedState.WAITING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                backgroundColor = Color.LightGray,
                                strokeWidth = 2.dp,
                                color = MaterialTheme.appColors.primary
                            )
                        }
                        IconButton(
                            modifier = iconModifier.padding(top = 2.dp),
                            onClick = { onDownloadClick(block) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(id = R.string.course_accessibility_stop_downloading_course_section),
                                tint = MaterialTheme.appColors.error
                            )
                        }
                    }
                }
                CardArrow(
                    degrees = 0f
                )
            }
        }
    }
}

private fun getUnitBlockIcon(block: Block): Int {
    return when (block.descendantsType) {
        BlockType.VIDEO -> R.drawable.ic_course_video
        BlockType.PROBLEM -> R.drawable.ic_course_pen
        BlockType.DISCUSSION -> R.drawable.ic_course_discussion
        else -> R.drawable.ic_course_block
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseSectionScreenPreview() {
    OpenEdXTheme {
        CourseSectionScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseSectionUIState.Blocks(
                listOf(
                    mockBlock,
                    mockBlock,
                    mockBlock,
                    mockBlock
                ),
                mapOf(),
                "",
                "Course default"
            ),
            uiMessage = null,
            onBackClick = {},
            onItemClick = {},
            onDownloadClick = {},
            onGoToPrerequisiteClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseSectionScreenTabletPreview() {
    OpenEdXTheme {
        CourseSectionScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseSectionUIState.Blocks(
                listOf(
                    mockBlock,
                    mockBlock,
                    mockBlock,
                    mockBlock
                ),
                mapOf(),
                "",
                "Course default",
            ),
            uiMessage = null,
            onBackClick = {},
            onItemClick = {},
            onDownloadClick = {},
            onGoToPrerequisiteClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseSectionScreenGatedPreview() {
    OpenEdXTheme {
        CourseSectionScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseSectionUIState.Gated(
                "Gated Subsection",
                "Prerequisite Subsection",
                "Prerequisite Id"
            ),
            uiMessage = null,
            onBackClick = {},
            onItemClick = {},
            onDownloadClick = {},
            onGoToPrerequisiteClick = {}
        )
    }
}

private val mockBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.HTML,
    displayName = "Block",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(0),
    descendants = emptyList(),
    descendantsType = BlockType.HTML,
    completion = 0.0,
    containsGatedContent = false
)

package com.vitorpamplona.amethyst.ui.screen.loggedIn

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.amethyst.LocalPreferences
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.ui.navigation.SendButton
import com.vitorpamplona.amethyst.ui.screen.NostrHiddenAccountsFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrHiddenWordsFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrSpammerAccountsFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.RefresheableView
import com.vitorpamplona.amethyst.ui.screen.RefreshingFeedUserFeedView
import com.vitorpamplona.amethyst.ui.screen.StringFeedView
import com.vitorpamplona.amethyst.ui.screen.UserFeedViewModel
import com.vitorpamplona.amethyst.ui.theme.ButtonBorder
import com.vitorpamplona.amethyst.ui.theme.StdPadding
import com.vitorpamplona.amethyst.ui.theme.TabRowHeight
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HiddenUsersScreen(accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val hiddenFeedViewModel: NostrHiddenAccountsFeedViewModel = viewModel(
        factory = NostrHiddenAccountsFeedViewModel.Factory(accountViewModel.account)
    )

    val hiddenWordsFeedViewModel: NostrHiddenWordsFeedViewModel = viewModel(
        factory = NostrHiddenWordsFeedViewModel.Factory(accountViewModel.account)
    )

    val spammerFeedViewModel: NostrSpammerAccountsFeedViewModel = viewModel(
        factory = NostrSpammerAccountsFeedViewModel.Factory(accountViewModel.account)
    )

    HiddenUsersScreen(hiddenFeedViewModel, hiddenWordsFeedViewModel, spammerFeedViewModel, accountViewModel, nav)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenUsersScreen(
    hiddenFeedViewModel: NostrHiddenAccountsFeedViewModel,
    hiddenWordsViewModel: NostrHiddenWordsFeedViewModel,
    spammerFeedViewModel: NostrSpammerAccountsFeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val lifeCycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("Hidden Users Start")
                hiddenWordsViewModel.invalidateData()
                hiddenFeedViewModel.invalidateData()
                spammerFeedViewModel.invalidateData()
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(Modifier.fillMaxHeight()) {
        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
            val pagerState = rememberPagerState() { 3 }
            val coroutineScope = rememberCoroutineScope()
            var warnAboutReports by remember { mutableStateOf(accountViewModel.account.warnAboutPostsWithReports) }
            var filterSpam by remember { mutableStateOf(accountViewModel.account.filterSpamFromStrangers) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = warnAboutReports,
                    onCheckedChange = {
                        warnAboutReports = it
                        accountViewModel.account.updateOptOutOptions(warnAboutReports, filterSpam)
                        LocalPreferences.saveToEncryptedStorage(accountViewModel.account)
                    }
                )

                Text(stringResource(R.string.warn_when_posts_have_reports_from_your_follows))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = filterSpam,
                    onCheckedChange = {
                        filterSpam = it
                        accountViewModel.account.updateOptOutOptions(warnAboutReports, filterSpam)
                        LocalPreferences.saveToEncryptedStorage(accountViewModel.account)
                    }
                )

                Text(stringResource(R.string.filter_spam_from_strangers))
            }

            ScrollableTabRow(
                backgroundColor = MaterialTheme.colors.background,
                edgePadding = 8.dp,
                selectedTabIndex = pagerState.currentPage,
                modifier = TabRowHeight
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = {
                        Text(text = stringResource(R.string.blocked_users))
                    }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = {
                        Text(text = stringResource(R.string.spamming_users))
                    }
                )
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                    text = {
                        Text(text = stringResource(R.string.hidden_words))
                    }
                )
            }
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> RefreshingUserFeedView(hiddenFeedViewModel, accountViewModel) {
                        RefreshingFeedUserFeedView(hiddenFeedViewModel, accountViewModel, nav)
                    }
                    1 -> RefreshingUserFeedView(spammerFeedViewModel, accountViewModel) {
                        RefreshingFeedUserFeedView(spammerFeedViewModel, accountViewModel, nav)
                    }
                    2 -> HiddenWordsFeed(hiddenWordsViewModel, accountViewModel)
                }
            }
        }
    }
}

@Composable
private fun HiddenWordsFeed(
    hiddenWordsViewModel: NostrHiddenWordsFeedViewModel,
    accountViewModel: AccountViewModel
) {
    RefresheableView(hiddenWordsViewModel, false) {
        StringFeedView(
            hiddenWordsViewModel,
            post = { AddMuteWordTextField(accountViewModel) }
        ) {
            MutedWordHeader(tag = it, account = accountViewModel)
        }
    }
}

@Composable
private fun AddMuteWordTextField(accountViewModel: AccountViewModel) {
    Row {
        val currentWordToAdd = remember {
            mutableStateOf("")
        }
        val hasChanged by remember {
            derivedStateOf {
                currentWordToAdd.value != ""
            }
        }

        OutlinedTextField(
            value = currentWordToAdd.value,
            onValueChange = { currentWordToAdd.value = it },
            label = { Text(text = stringResource(R.string.hide_new_word_label)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.hide_new_word_label),
                    color = MaterialTheme.colors.placeholderText
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    accountViewModel.hide(currentWordToAdd.value)
                    currentWordToAdd.value = ""
                }
            ),
            singleLine = true,
            trailingIcon = {
                if (hasChanged) {
                    SendButton {
                        accountViewModel.hide(currentWordToAdd.value)
                        currentWordToAdd.value = ""
                    }
                }
            }
        )
    }
}

@Composable
fun RefreshingUserFeedView(
    feedViewModel: UserFeedViewModel,
    accountViewModel: AccountViewModel,
    inner: @Composable () -> Unit
) {
    WatchAccountAndBlockList(feedViewModel, accountViewModel)
    inner()
}

@Composable
fun WatchAccountAndBlockList(
    feedViewModel: UserFeedViewModel,
    accountViewModel: AccountViewModel
) {
    val accountState by accountViewModel.accountLiveData.observeAsState()
    val blockListState by accountViewModel.account.getBlockListNote().live().metadata.observeAsState()

    LaunchedEffect(accountViewModel, accountState, blockListState) {
        feedViewModel.invalidateData()
    }
}

@Composable
fun MutedWordHeader(tag: String, modifier: Modifier = StdPadding, account: AccountViewModel) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        Column(modifier = modifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    tag,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                MutedWordActionOptions(tag, account)
            }
        }

        Divider(
            thickness = 0.25.dp
        )
    }
}

@Composable
fun MutedWordActionOptions(
    word: String,
    accountViewModel: AccountViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isMutedWord by accountViewModel.account.liveHiddenUsers.map {
        word in it.hiddenWords
    }.distinctUntilChanged().observeAsState()

    if (isMutedWord == true) {
        ShowWordButton {
            if (!accountViewModel.isWriteable()) {
                if (accountViewModel.loggedInWithExternalSigner()) {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.account.showWord(word)
                    }
                } else {
                    scope.launch {
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.login_with_a_private_key_to_be_able_to_unfollow),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            } else {
                scope.launch(Dispatchers.IO) {
                    accountViewModel.account.showWord(word)
                }
            }
        }
    } else {
        HideWordButton {
            if (!accountViewModel.isWriteable()) {
                if (accountViewModel.loggedInWithExternalSigner()) {
                    scope.launch(Dispatchers.IO) {
                        accountViewModel.account.hideWord(word)
                    }
                } else {
                    scope.launch {
                        Toast
                            .makeText(
                                context,
                                context.getString(R.string.login_with_a_private_key_to_be_able_to_follow),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            } else {
                scope.launch(Dispatchers.IO) {
                    accountViewModel.account.hideWord(word)
                }
            }
        }
    }
}

@Composable
fun HideWordButton(onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(horizontal = 3.dp),
        onClick = onClick,
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = stringResource(R.string.block_only), color = Color.White)
    }
}

@Composable
fun ShowWordButton(text: Int = R.string.unblock, onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(start = 3.dp),
        onClick = onClick,
        shape = ButtonBorder,
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            ),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Text(text = stringResource(text), color = Color.White, textAlign = TextAlign.Center)
    }
}

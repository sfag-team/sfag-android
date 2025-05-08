package com.droidhen.formalautosim.presentation.navigation.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidhen.formalautosim.R
import com.droidhen.formalautosim.core.viewModel.SplashViewModel
import com.droidhen.formalautosim.presentation.theme.signScreenSpacerModifierMedium
import com.droidhen.formalautosim.presentation.theme.signScreenSpacerModifierSmall
import views.ConfirmPasswordTextField
import views.DefaultTextField
import views.FASButton
import views.IssueMessage
import views.PasswordTextField

@Composable
fun SignInScreen(navigateToMainActivity: () -> Unit) {
    val viewModel: SplashViewModel = hiltViewModel()
    val signToggleChoseModifier = Modifier
        .border(4.dp, color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(8.dp))
        .shadow(3.dp, RoundedCornerShape(4.dp), true, MaterialTheme.colorScheme.surface)
        .padding(10.dp)
    val signToggleUsualModifier = Modifier
        .border(2.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
        .padding(8.dp)
    var recomposeKey by remember {
        mutableIntStateOf(0)
    }
    var showIssueMessage by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current


    if(viewModel.checkIfUserAuthorised()){
        LaunchedEffect(Unit) {
            navigateToMainActivity()
        }
    }else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.size(80.dp))
                    Row(
                        modifier = Modifier
                            .height(150.dp)
                            .padding(start = 32.dp, end = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(id = com.droidhen.theme.R.mipmap.icon),
                            contentDescription = stringResource(
                                R.string.icon_image
                            ),
                            contentScale = ContentScale.FillHeight,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(2f)
                        )
                        Text(
                            text = stringResource(id = R.string.app_name),
                            textAlign = TextAlign.Center,
                            fontSize = 64.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                shadow = Shadow(
                                    color = MaterialTheme.colorScheme.secondary,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 8f
                                )
                            )
                        )
                    }
                    Spacer(modifier = signScreenSpacerModifierMedium)
                    key(recomposeKey) {
                        Row(
                            modifier = Modifier
                                .height(45.dp)
                                .width(240.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.sign_in
                                ),
                                modifier = (if (viewModel.isSignIn) signToggleChoseModifier else signToggleUsualModifier)
                                    .weight(1f)
                                    .clickable {
                                        recomposeKey++
                                        viewModel.isSignIn = true
                                    },
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = signScreenSpacerModifierMedium)
                            Text(
                                text = stringResource(R.string.sign_up),
                                modifier = (if (viewModel.isSignIn) signToggleUsualModifier else signToggleChoseModifier)
                                    .weight(1f)
                                    .clickable {
                                        recomposeKey++
                                        viewModel.isSignIn = false
                                    },
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = signScreenSpacerModifierSmall)

                        if (!viewModel.isSignIn) {
                            DefaultTextField(
                                hint = "name",
                                value = viewModel.getName().value,
                                requirementText = stringResource(R.string.name_requirement),
                                { name ->
                                    viewModel.setName(name)
                                },
                                { viewModel.checkNameRequirements() })
                        }

                        DefaultTextField(hint = stringResource(
                            id = R.string.email
                        ),
                            value = viewModel.getEmail().value,
                            requirementText = stringResource(id = R.string.email_requirements),
                            { email ->
                                viewModel.setEmail(email)
                            },
                            { return@DefaultTextField viewModel.checkEmailRequirements() })

                        PasswordTextField(
                            password = viewModel.getPassword().value,
                            { password ->
                                viewModel.setPassword(password)
                            },
                            viewModel.getPasswordShowed(),
                            { viewModel.checkSinglePasswordRequirements() })

                        if (!viewModel.isSignIn) {
                            ConfirmPasswordTextField(
                                password = viewModel.getSecondPassword().value,
                                { secondPassword ->
                                    viewModel.setSecondPassword(secondPassword)
                                },
                                { return@ConfirmPasswordTextField viewModel.checkPasswordRequirements() },
                                viewModel.getPasswordShowed().value
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(if (viewModel.isSignIn) 80.dp else 30.dp))
                    Text(
                        text = stringResource(id = R.string.stay_in_guest_mode),
                        color = MaterialTheme.colorScheme.surface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(24.dp)
                            .width(196.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.secondary)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small
                            )
                            .clickable {
                                viewModel.saveUserNonAuthorised()
                                navigateToMainActivity()
                            }

                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    FASButton(
                        text = stringResource(id = if (viewModel.isSignIn) R.string.sign_in else R.string.sign_up),
                        enabled = viewModel.isButtonEnabled()
                    ) {
                        viewModel.buttonPressed({
                            navigateToMainActivity()
                        }, {
                            Toast.makeText(
                                context,
                                if (viewModel.isSignIn) context.getString(R.string.incorrect_sign_in_data) else context.getString(
                                    R.string.incorrect_sign_up_data
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }, {
                            showIssueMessage = true
                        })
                    }
                    Spacer(modifier = Modifier.height(90.dp))
                }

            }
            if (showIssueMessage) {
                IssueMessage({
                    showIssueMessage = false
                }, {
                    navigateToMainActivity()
                })
            }
        }
    }
}


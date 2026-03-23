package com.spencehouse.logue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spencehouse.logue.ui.model.LoginViewModel
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val autofill = androidx.compose.ui.platform.LocalAutofill.current
    val autofillTree = androidx.compose.ui.platform.LocalAutofillTree.current

    val usernameAutofillNode = remember {
        androidx.compose.ui.autofill.AutofillNode(
            autofillTypes = listOf(androidx.compose.ui.autofill.AutofillType.EmailAddress),
            onFill = viewModel::onUsernameChange
        )
    }
    val passwordAutofillNode = remember {
        androidx.compose.ui.autofill.AutofillNode(
            autofillTypes = listOf(androidx.compose.ui.autofill.AutofillType.Password),
            onFill = viewModel::onPasswordChange
        )
    }

    LaunchedEffect(Unit) {
        autofillTree += usernameAutofillNode
        autofillTree += passwordAutofillNode
    }

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess()
            viewModel.onLoginComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Logue Login",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = uiState.username,
            onValueChange = viewModel::onUsernameChange,
            label = { Text("HondaLink Email Address") },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    usernameAutofillNode.boundingBox = it.boundsInWindow()
                }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        autofill?.requestAutofillForNode(usernameAutofillNode)
                    } else {
                        autofill?.cancelAutofillForNode(usernameAutofillNode)
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("HondaLink Password") },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    passwordAutofillNode.boundingBox = it.boundsInWindow()
                }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        autofill?.requestAutofillForNode(passwordAutofillNode)
                    } else {
                        autofill?.cancelAutofillForNode(passwordAutofillNode)
                    }
                },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = viewModel::login,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }

        uiState.error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

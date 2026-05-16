package com.forzaball.feature.auth.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.forzaball.R
import com.forzaball.feature.auth.AuthValidation
import com.forzaball.ui.theme.ForzaBallPrimary

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignUpWithEmail: (firstName: String, lastName: String, email: String, phone: String, password: String) -> Unit,
    onSignUpWithGoogle: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var firstNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var lastNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ForzaBallPrimary.copy(alpha = 0.5f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = ForzaBallPrimary,
        cursorColor = ForzaBallPrimary,
        focusedTrailingIconColor = ForzaBallPrimary,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Back")
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = R.drawable.app_logo,
                    contentDescription = "ForzaBall",
                    modifier = Modifier.height(36.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp)
                .background(
                    ForzaBallPrimary.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp),
                ),
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Sign Up",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            text = "Join the global football community today",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Button(
                onClick = { },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Sign Up", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onSignIn,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Log In", fontWeight = FontWeight.SemiBold)
            }
        }

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it; firstNameError = null },
            label = { Text("First name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            singleLine = true,
            isError = firstNameError != null,
            supportingText = firstNameError?.let { { Text(it) } },
            colors = fieldColors,
        )
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it; lastNameError = null },
            label = { Text("Last name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            singleLine = true,
            isError = lastNameError != null,
            supportingText = lastNameError?.let { { Text(it) } },
            colors = fieldColors,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            colors = fieldColors,
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; phoneError = null },
            label = { Text("Phone") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phoneError != null,
            supportingText = phoneError?.let { { Text(it) } },
            colors = fieldColors,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            colors = fieldColors,
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; confirmPasswordError = null },
            label = { Text("Confirm password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                    )
                }
            },
            isError = confirmPasswordError != null,
            supportingText = confirmPasswordError?.let { { Text(it) } },
            colors = fieldColors,
        )

        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }

        Button(
            onClick = {
                firstNameError = AuthValidation.validateFirstName(firstName)
                lastNameError = AuthValidation.validateLastName(lastName)
                emailError = AuthValidation.validateEmail(email)
                phoneError = AuthValidation.validatePhone(phone)
                passwordError = AuthValidation.validatePassword(password)
                confirmPasswordError = AuthValidation.validateConfirmPassword(password, confirmPassword)
                if (firstNameError == null && lastNameError == null && emailError == null &&
                    phoneError == null && passwordError == null && confirmPasswordError == null
                ) {
                    onSignUpWithEmail(firstName.trim(), lastName.trim(), email.trim(), phone.trim(), password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(48.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = ForzaBallPrimary),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Continue with Email", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = onSignUpWithGoogle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .height(48.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Continue with Google", fontWeight = FontWeight.SemiBold)
        }

        Text(
            text = "By continuing, you agree to ForzaBall's Terms of Service and Privacy Policy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(24.dp),
        )
    }
}

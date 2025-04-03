package com.mydoan.bachkimthanbao;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.mydoan.bachkimthanbao.databinding.ActivitySignUpBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ViewBinding
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Set up the gender dropdown using Spinner
        String[] genderOptions = getResources().getStringArray(R.array.gender_options);
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderOptions);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGender.setAdapter(genderAdapter);

        // Set up DatePickerDialog for Date of Birth
        binding.etDob.setOnClickListener(v -> showDatePicker());

        // Set up sign-up button click event
        binding.btnSignUp.setOnClickListener(v -> registerUser());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                SignUpActivity.this,
                (view, year1, month1, dayOfMonth) -> {
                    // Format and display selected date
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, month1, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    binding.etDob.setText(sdf.format(selectedDate.getTime()));
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void registerUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        String gender = binding.spinnerGender.getSelectedItem().toString(); // Get selected gender
        String dob = binding.etDob.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Vui lòng nhập họ và tên!");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Vui lòng nhập email!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Vui lòng nhập mật khẩu!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Mật khẩu xác nhận không khớp!");
            return;
        }

        if (TextUtils.isEmpty(gender)) {
            binding.tilGender.setError("Vui lòng chọn giới tính!");
            return;
        }

        if (TextUtils.isEmpty(dob)) {
            binding.tilDob.setError("Vui lòng chọn ngày sinh!");
            return;
        }

        // Create Firebase user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Update user's profile with name
                        if (!TextUtils.isEmpty(name)) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            if (mAuth.getCurrentUser() != null) {
                                mAuth.getCurrentUser().updateProfile(profileUpdates);
                            }
                        }

                        Toast.makeText(SignUpActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                        finish(); // Return to login screen
                    } else {
                        // Handle Firebase registration errors
                        String errorMessage = "Đăng ký thất bại: ";
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthWeakPasswordException e) {
                            errorMessage += "Mật khẩu quá yếu!";
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            errorMessage += "Email không hợp lệ!";
                        } catch (FirebaseAuthUserCollisionException e) {
                            errorMessage += "Email đã được sử dụng!";
                        } catch (Exception e) {
                            errorMessage += e.getMessage();
                        }
                        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}

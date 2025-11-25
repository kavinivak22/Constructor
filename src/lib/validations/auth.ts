import { z } from 'zod';

// Password validation with strong requirements
const passwordSchema = z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(100, 'Password must not exceed 100 characters')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
    .regex(/[0-9]/, 'Password must contain at least one number');

// Schema for user signup
export const signupSchema = z
    .object({
        email: z
            .string()
            .email('Invalid email address')
            .max(255, 'Email must not exceed 255 characters')
            .toLowerCase()
            .trim(),
        password: passwordSchema,
        confirmPassword: z.string(),
        fullName: z
            .string()
            .min(2, 'Full name must be at least 2 characters')
            .max(100, 'Full name must not exceed 100 characters')
            .trim(),
    })
    .refine((data) => data.password === data.confirmPassword, {
        message: 'Passwords do not match',
        path: ['confirmPassword'],
    });

// Schema for user login
export const loginSchema = z.object({
    email: z
        .string()
        .email('Invalid email address')
        .max(255, 'Email must not exceed 255 characters')
        .toLowerCase()
        .trim(),
    password: z.string().min(1, 'Password is required'),
});

// Schema for password reset request
export const passwordResetRequestSchema = z.object({
    email: z
        .string()
        .email('Invalid email address')
        .max(255, 'Email must not exceed 255 characters')
        .toLowerCase()
        .trim(),
});

// Schema for password reset with new password
export const passwordResetSchema = z
    .object({
        password: passwordSchema,
        confirmPassword: z.string(),
    })
    .refine((data) => data.password === data.confirmPassword, {
        message: 'Passwords do not match',
        path: ['confirmPassword'],
    });

// Schema for updating password when logged in
export const passwordUpdateSchema = z
    .object({
        currentPassword: z.string().min(1, 'Current password is required'),
        newPassword: passwordSchema,
        confirmNewPassword: z.string(),
    })
    .refine((data) => data.newPassword === data.confirmNewPassword, {
        message: 'Passwords do not match',
        path: ['confirmNewPassword'],
    })
    .refine((data) => data.currentPassword !== data.newPassword, {
        message: 'New password must be different from current password',
        path: ['newPassword'],
    });

// Schema for company registration
export const companyRegistrationSchema = z.object({
    name: z
        .string()
        .min(2, 'Company name must be at least 2 characters')
        .max(200, 'Company name must not exceed 200 characters')
        .trim(),
    address: z
        .string()
        .min(5, 'Address must be at least 5 characters')
        .max(300, 'Address must not exceed 300 characters')
        .trim()
        .optional(),
    phone: z
        .string()
        .regex(/^[+]?[(]?[0-9]{1,4}[)]?[-\s.]?[(]?[0-9]{1,4}[)]?[-\s.]?[0-9]{1,9}$/, 'Invalid phone number')
        .optional(),
    website: z
        .string()
        .url('Invalid website URL')
        .max(200, 'Website URL must not exceed 200 characters')
        .optional()
        .or(z.literal('')),
    businessType: z
        .string()
        .max(100, 'Business type must not exceed 100 characters')
        .optional(),
    companySize: z
        .enum(['1-10', '11-50', '51-200', '201-500', '500+'])
        .optional(),
});

// Type exports
export type SignupInput = z.infer<typeof signupSchema>;
export type LoginInput = z.infer<typeof loginSchema>;
export type PasswordResetRequestInput = z.infer<typeof passwordResetRequestSchema>;
export type PasswordResetInput = z.infer<typeof passwordResetSchema>;
export type PasswordUpdateInput = z.infer<typeof passwordUpdateSchema>;
export type CompanyRegistrationInput = z.infer<typeof companyRegistrationSchema>;

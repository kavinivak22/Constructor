import { z } from 'zod';

// User role enum
export const userRoleEnum = z.enum(['admin', 'member']);

// User status enum
export const userStatusEnum = z.enum(['active', 'inactive', 'pending']);

// Schema for inviting a new employee
export const employeeInviteSchema = z.object({
    email: z
        .string()
        .email('Invalid email address')
        .max(255, 'Email must not exceed 255 characters')
        .toLowerCase()
        .trim(),
    role: userRoleEnum.default('member'),
    projectIds: z
        .array(z.string().uuid('Invalid project ID'))
        .optional()
        .default([]),
    companyId: z.string().uuid('Invalid company ID'),
});

// Schema for updating employee details
export const employeeUpdateSchema = z.object({
    id: z.string().uuid('Invalid user ID'),
    displayName: z
        .string()
        .min(2, 'Display name must be at least 2 characters')
        .max(100, 'Display name must not exceed 100 characters')
        .optional(),
    phone: z
        .string()
        .regex(/^[+]?[(]?[0-9]{1,4}[)]?[-\s.]?[(]?[0-9]{1,4}[)]?[-\s.]?[0-9]{1,9}$/, 'Invalid phone number')
        .optional()
        .nullable(),
    role: userRoleEnum.optional(),
    status: userStatusEnum.optional(),
    projectIds: z.array(z.string().uuid('Invalid project ID')).optional(),
});

// Schema for assigning projects to employees
export const employeeProjectAssignmentSchema = z.object({
    userId: z.string().uuid('Invalid user ID'),
    projectIds: z.array(z.string().uuid('Invalid project ID')),
});

// Schema for removing employee from projects
export const employeeProjectRemovalSchema = z.object({
    userId: z.string().uuid('Invalid user ID'),
    projectId: z.string().uuid('Invalid project ID'),
});

// Schema for profile change requests (for member users)
export const profileChangeRequestSchema = z.object({
    userId: z.string().uuid('Invalid user ID'),
    fieldName: z.enum(['displayName', 'phone']),
    newValue: z.string().max(100, 'Value must not exceed 100 characters'),
    companyId: z.string().uuid('Invalid company ID'),
});

// Type exports
export type EmployeeInviteInput = z.infer<typeof employeeInviteSchema>;
export type EmployeeUpdateInput = z.infer<typeof employeeUpdateSchema>;
export type EmployeeProjectAssignmentInput = z.infer<typeof employeeProjectAssignmentSchema>;
export type EmployeeProjectRemovalInput = z.infer<typeof employeeProjectRemovalSchema>;
export type ProfileChangeRequestInput = z.infer<typeof profileChangeRequestSchema>;
export type UserRole = z.infer<typeof userRoleEnum>;
export type UserStatus = z.infer<typeof userStatusEnum>;

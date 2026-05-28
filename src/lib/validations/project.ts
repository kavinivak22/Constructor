import { z } from 'zod';

// Project status enum
export const projectStatusEnum = z.enum([
    'planning',
    'in-progress',
    'on-hold',
    'completed',
    'cancelled',
]);

// Project type enum
export const projectTypeEnum = z.enum([
    'residential',
    'commercial',
    'industrial',
    'infrastructure',
    'renovation',
    'other',
]);

// Base project schema with common fields
const baseProjectSchema = {
    name: z
        .string()
        .min(3, 'Project name must be at least 3 characters')
        .max(200, 'Project name must not exceed 200 characters')
        .trim(),
    description: z
        .string()
        .max(2000, 'Description must not exceed 2000 characters')
        .optional()
        .nullable(),
    startDate: z
        .string()
        .refine((date) => !isNaN(Date.parse(date)), 'Invalid start date')
        .optional()
        .nullable(),
    endDate: z
        .string()
        .refine((date) => !isNaN(Date.parse(date)), 'Invalid end date')
        .optional()
        .nullable(),
    status: projectStatusEnum.default('planning'),
    progress: z
        .number()
        .min(0, 'Progress must be at least 0')
        .max(100, 'Progress cannot exceed 100')
        .default(0),
    budget: z
        .number()
        .positive('Budget must be a positive number')
        .max(1_000_000_000, 'Budget cannot exceed 1 billion')
        .optional()
        .nullable(),
    clientName: z
        .string()
        .min(2, 'Client name must be at least 2 characters')
        .max(100, 'Client name must not exceed 100 characters')
        .optional()
        .nullable(),
    clientContact: z
        .string()
        .max(100, 'Client contact must not exceed 100 characters')
        .optional()
        .nullable(),
    location: z
        .string()
        .max(200, 'Location must not exceed 200 characters')
        .optional()
        .nullable(),
    projectType: projectTypeEnum.optional().nullable(),
    companyLogo: z.string().url().optional().nullable(),
};

// Schema for creating a new project
export const projectCreateSchema = z
    .object({
        ...baseProjectSchema,
        companyId: z.string().uuid('Invalid company ID'),
    })
    .refine(
        (data) => {
            // Ensure end date is after start date if both are provided
            if (data.startDate && data.endDate) {
                return new Date(data.endDate) >= new Date(data.startDate);
            }
            return true;
        },
        {
            message: 'End date must be after or equal to start date',
            path: ['endDate'],
        }
    );

// Schema for updating an existing project
export const projectUpdateSchema = z
    .object({
        id: z.string().uuid('Invalid project ID'),
        ...baseProjectSchema,
    })
    .partial()
    .required({ id: true })
    .refine(
        (data) => {
            // Ensure end date is after start date if both are provided
            if (data.startDate && data.endDate) {
                return new Date(data.endDate) >= new Date(data.startDate);
            }
            return true;
        },
        {
            message: 'End date must be after or equal to start date',
            path: ['endDate'],
        }
    );

// Schema for filtering projects
export const projectFilterSchema = z.object({
    status: projectStatusEnum.optional(),
    projectType: projectTypeEnum.optional(),
    search: z.string().max(200).optional(),
    sortBy: z.enum(['name', 'startDate', 'endDate', 'progress', 'budget']).optional(),
    sortOrder: z.enum(['asc', 'desc']).optional(),
});

// Type exports for TypeScript
export type ProjectCreateInput = z.infer<typeof projectCreateSchema>;
export type ProjectUpdateInput = z.infer<typeof projectUpdateSchema>;
export type ProjectFilterInput = z.infer<typeof projectFilterSchema>;
export type ProjectStatus = z.infer<typeof projectStatusEnum>;
export type ProjectType = z.infer<typeof projectTypeEnum>;

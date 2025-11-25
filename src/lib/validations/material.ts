import { z } from 'zod';

// Material unit enum
export const materialUnitEnum = z.enum([
    'pieces',
    'kg',
    'ton',
    'meter',
    'sqm',
    'cum',
    'liter',
    'bag',
    'box',
    'bundle',
    'other',
]);

// Schema for AI material estimation input
export const materialEstimationSchema = z.object({
    projectName: z
        .string()
        .min(3, 'Project name must be at least 3 characters')
        .max(200, 'Project name must not exceed 200 characters')
        .trim(),
    projectType: z
        .string()
        .min(3, 'Project type must be at least 3 characters')
        .max(100, 'Project type must not exceed 100 characters')
        .trim(),
    description: z
        .string()
        .min(10, 'Description must be at least 10 characters')
        .max(2000, 'Description must not exceed 2000 characters')
        .trim(),
    area: z
        .number()
        .positive('Area must be a positive number')
        .max(1_000_000, 'Area seems unreasonably large')
        .optional(),
    floors: z
        .number()
        .int('Number of floors must be a whole number')
        .positive('Number of floors must be positive')
        .max(200, 'Number of floors seems unreasonably high')
        .optional(),
    budget: z
        .number()
        .positive('Budget must be a positive number')
        .max(1_000_000_000, 'Budget cannot exceed 1 billion')
        .optional(),
    additionalRequirements: z
        .string()
        .max(1000, 'Additional requirements must not exceed 1000 characters')
        .optional(),
});

// Schema for creating/updating material items in inventory
export const materialItemSchema = z.object({
    name: z
        .string()
        .min(2, 'Material name must be at least 2 characters')
        .max(100, 'Material name must not exceed 100 characters')
        .trim(),
    category: z
        .string()
        .min(2, 'Category must be at least 2 characters')
        .max(50, 'Category must not exceed 50 characters')
        .trim(),
    quantity: z
        .number()
        .nonnegative('Quantity cannot be negative')
        .max(1_000_000, 'Quantity seems unreasonably high'),
    unit: materialUnitEnum,
    unitPrice: z
        .number()
        .positive('Unit price must be a positive number')
        .max(10_000_000, 'Unit price seems unreasonably high'),
    supplier: z
        .string()
        .min(2, 'Supplier name must be at least 2 characters')
        .max(100, 'Supplier name must not exceed 100 characters')
        .trim()
        .optional(),
    description: z
        .string()
        .max(500, 'Description must not exceed 500 characters')
        .optional(),
    projectId: z.string().uuid('Invalid project ID').optional(),
});

// Schema for updating material quantity (stock management)
export const materialQuantityUpdateSchema = z.object({
    id: z.string().uuid('Invalid material ID'),
    quantityChange: z
        .number()
        .refine((val) => val !== 0, 'Quantity change cannot be zero'),
    reason: z
        .string()
        .min(3, 'Reason must be at least 3 characters')
        .max(200, 'Reason must not exceed 200 characters')
        .trim(),
});

// Type exports
export type MaterialEstimationInput = z.infer<typeof materialEstimationSchema>;
export type MaterialItemInput = z.infer<typeof materialItemSchema>;
export type MaterialQuantityUpdateInput = z.infer<typeof materialQuantityUpdateSchema>;
export type MaterialUnit = z.infer<typeof materialUnitEnum>;

import { z } from 'zod';

// Expense category enum
export const expenseCategoryEnum = z.enum([
    'materials',
    'labor',
    'equipment',
    'permits',
    'utilities',
    'transportation',
    'subcontractor',
    'other',
]);

// Payment status enum
export const paymentStatusEnum = z.enum(['pending', 'paid', 'overdue', 'cancelled']);

// Base expense schema
const baseExpenseSchema = {
    projectId: z.string().uuid('Invalid project ID'),
    amount: z
        .number()
        .positive('Amount must be a positive number')
        .max(10_000_000, 'Amount cannot exceed 10 million'),
    category: expenseCategoryEnum,
    description: z
        .string()
        .min(3, 'Description must be at least 3 characters')
        .max(500, 'Description must not exceed 500 characters')
        .trim(),
    receiver: z
        .string()
        .min(2, 'Receiver name must be at least 2 characters')
        .max(100, 'Receiver name must not exceed 100 characters')
        .trim(),
    date: z
        .string()
        .refine((date) => !isNaN(Date.parse(date)), 'Invalid date')
        .refine((date) => new Date(date) <= new Date(), 'Date cannot be in the future'),
    paymentStatus: paymentStatusEnum.default('pending'),
    notes: z
        .string()
        .max(1000, 'Notes must not exceed 1000 characters')
        .optional()
        .nullable(),
    receiptUrl: z
        .string()
        .url('Invalid receipt URL')
        .optional()
        .nullable(),
};

// Schema for creating a new expense
export const expenseCreateSchema = z.object({
    ...baseExpenseSchema,
    userId: z.string().uuid('Invalid user ID'),
});

// Schema for updating an existing expense
export const expenseUpdateSchema = z
    .object({
        id: z.string().uuid('Invalid expense ID'),
        ...baseExpenseSchema,
    })
    .partial()
    .required({ id: true });

// Schema for filtering expenses
export const expenseFilterSchema = z.object({
    projectId: z.string().uuid('Invalid project ID').optional(),
    category: expenseCategoryEnum.optional(),
    paymentStatus: paymentStatusEnum.optional(),
    startDate: z
        .string()
        .refine((date) => !isNaN(Date.parse(date)), 'Invalid start date')
        .optional(),
    endDate: z
        .string()
        .refine((date) => !isNaN(Date.parse(date)), 'Invalid end date')
        .optional(),
    minAmount: z.number().positive().optional(),
    maxAmount: z.number().positive().optional(),
    userId: z.string().uuid('Invalid user ID').optional(),
    search: z.string().max(200).optional(),
    sortBy: z.enum(['date', 'amount', 'category', 'paymentStatus']).optional(),
    sortOrder: z.enum(['asc', 'desc']).optional(),
});

// Schema for deleting an expense
export const expenseDeleteSchema = z.object({
    id: z.string().uuid('Invalid expense ID'),
    projectId: z.string().uuid('Invalid project ID'),
});

// Type exports
export type ExpenseCreateInput = z.infer<typeof expenseCreateSchema>;
export type ExpenseUpdateInput = z.infer<typeof expenseUpdateSchema>;
export type ExpenseFilterInput = z.infer<typeof expenseFilterSchema>;
export type ExpenseDeleteInput = z.infer<typeof expenseDeleteSchema>;
export type ExpenseCategory = z.infer<typeof expenseCategoryEnum>;
export type PaymentStatus = z.infer<typeof paymentStatusEnum>;

'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Plus, Package, Receipt } from 'lucide-react';
import { useState } from 'react';
import { ExpenseFormSheet } from '@/components/expenses/expense-form-sheet';

export function QuickActions() {
    const [isExpenseSheetOpen, setIsExpenseSheetOpen] = useState(false);

    return (
        <>
            <div className="grid grid-cols-2 gap-2 mb-4 sm:flex sm:items-center sm:mb-2">
                <Link href="/worklog" className="w-full sm:w-auto">
                    <Button variant="outline" size="sm" className="gap-2 h-9 w-full sm:w-auto">
                        <Plus className="h-4 w-4 text-primary" />
                        Add Worklog
                    </Button>
                </Link>
                <Button
                    variant="outline"
                    size="sm"
                    className="gap-2 h-9 w-full sm:w-auto"
                    onClick={() => setIsExpenseSheetOpen(true)}
                >
                    <Receipt className="h-4 w-4 text-primary" />
                    Add Expense
                </Button>
                <Link href="/materials" className="col-span-2 sm:col-span-1 w-full sm:w-auto">
                    <Button variant="outline" size="sm" className="gap-2 h-9 w-full sm:w-auto">
                        <Package className="h-4 w-4 text-primary" />
                        View Materials
                    </Button>
                </Link>
            </div>

            <ExpenseFormSheet
                isOpen={isExpenseSheetOpen}
                setIsOpen={setIsExpenseSheetOpen}
                projectId={undefined}
                expense={null}
            />
        </>
    );
}

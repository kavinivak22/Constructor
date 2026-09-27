'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Plus, Package, Receipt } from 'lucide-react';
import { useState } from 'react';
import { ExpenseFormSheet } from '@/components/expenses/expense-form-sheet';
import { CreateWorklogDialog } from '@/components/worklog/create-worklog-dialog';

export function QuickActions() {
    const [isExpenseSheetOpen, setIsExpenseSheetOpen] = useState(false);

    return (
        <>
            <div className="grid grid-cols-3 gap-1.5 sm:gap-2.5">
                <CreateWorklogDialog
                    trigger={
                        <Button variant="outline" size="sm" className="gap-1.5 h-8 sm:h-9 text-xs font-medium px-2 w-full rounded-xl border-border/60 hover:bg-primary/5 hover:border-primary/30 transition-colors shadow-2xs">
                            <Plus className="h-3.5 w-3.5 text-primary shrink-0" />
                            <span className="truncate">Worklog</span>
                        </Button>
                    }
                />
                <Button
                    variant="outline"
                    size="sm"
                    className="gap-1.5 h-8 sm:h-9 text-xs font-medium px-2 w-full rounded-xl border-border/60 hover:bg-primary/5 hover:border-primary/30 transition-colors shadow-2xs"
                    onClick={() => setIsExpenseSheetOpen(true)}
                >
                    <Receipt className="h-3.5 w-3.5 text-primary shrink-0" />
                    <span className="truncate">Expense</span>
                </Button>
                <Link href="/materials" className="w-full">
                    <Button variant="outline" size="sm" className="gap-1.5 h-8 sm:h-9 text-xs font-medium px-2 w-full rounded-xl border-border/60 hover:bg-primary/5 hover:border-primary/30 transition-colors shadow-2xs">
                        <Package className="h-3.5 w-3.5 text-primary shrink-0" />
                        <span className="truncate">Materials</span>
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

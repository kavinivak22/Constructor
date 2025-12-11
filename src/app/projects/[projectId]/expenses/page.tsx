
'use client';

import { useState, useMemo, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { type Expense, type Project, type User as AppUser } from '@/lib/data';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Calendar } from '@/components/ui/calendar';
import { Card, CardContent } from '@/components/ui/card';
import { ArrowLeft, PlusCircle, MoreHorizontal, Calendar as CalendarIcon, Search, IndianRupee, User, CalendarDays, FileDown } from 'lucide-react';
import { format, startOfDay, endOfDay, subDays, startOfMonth, endOfMonth } from 'date-fns';
import { DateRange } from 'react-day-picker';
import { useToast } from '@/hooks/use-toast';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';
import jsPDF from 'jspdf';
import 'jspdf-autotable';
import { useSupabase } from '@/supabase/provider';
import { ExpenseFormSheet } from '@/components/expenses/expense-form-sheet';
import { useTranslation } from '@/lib/i18n/language-context';

interface jsPDFWithAutoTable extends jsPDF {
  autoTable: (options: any) => jsPDFWithAutoTable;
}

const categoryIcons: { [key: string]: React.ElementType } = {
  materials: require('lucide-react').Hammer,
  labor: require('lucide-react').Users,
  equipment: require('lucide-react').Truck,
  permits: require('lucide-react').FileText,
  subcontractor: require('lucide-react').HardHat,
  other: require('lucide-react').Wrench,
};

export default function ExpensesPage() {
  const { t } = useTranslation();
  const { projectId } = useParams();
  const router = useRouter();
  const { toast } = useToast();
  const { supabase, user } = useSupabase();

  const [isSheetOpen, setIsSheetOpen] = useState(false);
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null);
  const [expenseToDelete, setExpenseToDelete] = useState<Expense | null>(null);
  const [isDeleteAlertOpen, setIsDeleteAlertOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [userFilter, setUserFilter] = useState<'all' | 'me'>('all');
  const [dateRange, setDateRange] = useState<DateRange | undefined>();

  const [project, setProject] = useState<Project | null>(null);
  const [userProfile, setUserProfile] = useState<AppUser | null>(null);
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      if (!user || !projectId) return;
      setIsLoading(true);

      try {
        // Fetch project
        const { data: projectData, error: projectError } = await supabase
          .from('projects')
          .select('*')
          .eq('id', projectId)
          .single();

        if (projectError) throw projectError;
        setProject(projectData as Project);

        // Fetch user profile
        const { data: userData, error: userError } = await supabase
          .from('users')
          .select('*')
          .eq('id', user.id)
          .single();

        if (userError) throw userError;
        setUserProfile(userData as AppUser);

        // Fetch expenses with user info
        const { data: expensesData, error: expensesError } = await supabase
          .from('expenses')
          .select(`
            *,
            user:users!expenses_userid_fkey (
              name:displayName,
              photoURL
            )
          `)
          .eq('projectId', projectId)
          .order('expenseDate', { ascending: false });

        if (expensesError) throw expensesError;
        setExpenses(expensesData as Expense[]);
      } catch (error: any) {
        console.error('Error fetching data:', {
          message: error?.message,
          details: error?.details,
          hint: error?.hint,
          code: error?.code,
          fullError: error
        });

        // If expenses table doesn't exist, set empty array
        if (error?.code === '42P01' || error?.message?.includes('relation') || error?.message?.includes('does not exist')) {
          console.warn('Expenses table may not exist. Please create it using the SQL provided.');
          setExpenses([]);
        }
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [supabase, user, projectId]);

  const expenseCategories = useMemo(() => {
    if (!expenses) return [];
    return ['all', ...Array.from(new Set(expenses.map(e => e.category)))];
  }, [expenses]);

  const filteredExpenses = useMemo(() => {
    if (!expenses) return [];

    return expenses.filter(expense => {
      // Search filter
      const matchesSearch = expense.description.toLowerCase().includes(searchQuery.toLowerCase());

      // Category filter
      const matchesCategory = categoryFilter === 'all' || expense.category === categoryFilter;

      // User filter
      const matchesUser = userFilter === 'all' || expense.userId === user?.id;

      // Date range filter
      let matchesDate = true;
      if (dateRange?.from && dateRange?.to) {
        const expenseDate = expense.expenseDate.toDate
          ? expense.expenseDate.toDate()
          : (expense.expenseDate instanceof Date ? expense.expenseDate : new Date(expense.expenseDate as unknown as string));
        matchesDate = expenseDate >= dateRange.from && expenseDate <= dateRange.to;
      }

      return matchesSearch && matchesCategory && matchesUser && matchesDate;
    });
  }, [expenses, searchQuery, categoryFilter, userFilter, dateRange, user]);


  const handleEdit = (expense: Expense) => {
    setEditingExpense(expense);
    setIsSheetOpen(true);
  };

  const handleAddNew = () => {
    setEditingExpense(null);
    setIsSheetOpen(true);
  };

  const handleDeleteRequest = (expense: Expense) => {
    setExpenseToDelete(expense);
    setIsDeleteAlertOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!expenseToDelete) return;

    try {
      const { error } = await supabase
        .from('expenses')
        .delete()
        .eq('id', expenseToDelete.id);

      if (error) throw error;

      // Update local state
      setExpenses(expenses.filter(e => e.id !== expenseToDelete.id));

      toast({
        title: 'Expense Deleted',
        description: 'The expense has been successfully deleted.',
      });
    } catch (error) {
      console.error('Error deleting expense:', error);
      toast({
        title: 'Error',
        description: 'Failed to delete expense.',
        variant: 'destructive',
      });
    }

    setIsDeleteAlertOpen(false);
    setExpenseToDelete(null);
  };

  const handleExpenseUpdate = () => {
    // Refetch expenses after add/edit
    const fetchExpenses = async () => {
      if (!projectId) return;
      try {
        const { data: expensesData, error: expensesError } = await supabase
          .from('expenses')
          .select(`
            *,
            user:users!expenses_userid_fkey (
              name:displayName,
              photoURL
            )
          `)
          .eq('projectId', projectId)
          .order('expenseDate', { ascending: false });

        if (expensesError) throw expensesError;
        setExpenses(expensesData as Expense[]);
      } catch (error) {
        console.error('Error fetching expenses:', error);
      }
    };

    fetchExpenses();
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
    }).format(amount);
  };

  const formatDate = (date: Date, formatString = 'MMM d, yyyy') => {
    return format(date, formatString);
  };

  const getCategoryIcon = (category: string) => {
    const Icon = categoryIcons[category.toLowerCase()] || require('lucide-react').Wrench;
    return <Icon className="h-4 w-4 text-muted-foreground" />;
  };

  const handleExportPDF = () => {
    toast({ title: 'Exporting not available', description: 'This feature is not available in the demo.' });
  };


  const totalExpenses = useMemo(() => {
    return filteredExpenses.reduce((total, expense) => total + expense.amount, 0);
  }, [filteredExpenses]);


  return (
    <>
      <div className="flex flex-col h-full bg-secondary">
        <header className="flex items-center gap-4 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
          <Button variant="ghost" size="icon" onClick={() => router.back()}>
            <ArrowLeft className="w-5 h-5" />
          </Button>
          <div className="flex-1">
            <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
              {t('project_expenses.title')}
            </h1>
            <p className="text-sm text-muted-foreground">{project?.name || 'Loading...'}</p>
          </div>
          <Button onClick={handleAddNew}>
            <PlusCircle className="w-4 h-4 mr-2" />
            {t('project_expenses.add_expense')}
          </Button>
        </header>

        <main className="flex-1 p-4 overflow-y-auto md:p-6">
          <div className='max-w-4xl mx-auto'>
            <div className="mb-6 space-y-4">
              <Card className="w-full">
                <CardContent className="p-4 flex items-center gap-4">
                  <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-primary/10">
                    <IndianRupee className="w-6 h-6 text-primary" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">{t('project_expenses.total_spent')}</p>
                    <p className="text-2xl font-bold">{formatCurrency(totalExpenses)}</p>
                  </div>
                </CardContent>
              </Card>
              <div className="flex flex-col sm:flex-row gap-4">
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
                  <Input
                    placeholder={t('project_expenses.search_placeholder')}
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <div className="flex flex-col sm:flex-row flex-wrap gap-3">
                  <Popover>
                    <PopoverTrigger asChild>
                      <Button
                        id="date"
                        variant={"outline"}
                        className={cn(
                          "w-full sm:w-[260px] justify-start text-left font-normal",
                          !dateRange && "text-muted-foreground"
                        )}
                      >
                        <CalendarDays className="mr-2 h-4 w-4" />
                        {dateRange?.from ? (
                          dateRange.to ? (
                            <>
                              {format(dateRange.from, "LLL dd, y")} -{" "}
                              {format(dateRange.to, "LLL dd, y")}
                            </>
                          ) : (
                            format(dateRange.from, "LLL dd, y")
                          )
                        ) : (
                          <span>{t('common.filter')} by date...</span>
                        )}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="end">
                      <div className='flex'>
                        <div className='p-2 border-r'>
                          <div className='flex flex-col items-start'>
                            <Button variant="ghost" className='w-full justify-start' onClick={() => setDateRange({ from: startOfDay(new Date()), to: endOfDay(new Date()) })}>Today</Button>
                            <Button variant="ghost" className='w-full justify-start' onClick={() => setDateRange({ from: startOfDay(subDays(new Date(), 1)), to: endOfDay(subDays(new Date(), 1)) })}>Yesterday</Button>
                            <Button variant="ghost" className='w-full justify-start' onClick={() => setDateRange({ from: startOfDay(subDays(new Date(), 6)), to: endOfDay(new Date()) })}>Last 7 days</Button>
                            <Button variant="ghost" className='w-full justify-start' onClick={() => setDateRange({ from: startOfMonth(new Date()), to: endOfMonth(new Date()) })}>This Month</Button>
                            <Button variant="ghost" className='w-full justify-start' onClick={() => setDateRange({ from: startOfMonth(subDays(new Date(), 30)), to: endOfMonth(subDays(new Date(), 30)) })}>Last Month</Button>
                            <Button variant="ghost" className='w-full justify-start text-red-600' onClick={() => setDateRange(undefined)}>Clear</Button>
                          </div>
                        </div>
                        <Calendar
                          initialFocus
                          mode="range"
                          defaultMonth={dateRange?.from}
                          selected={dateRange}
                          onSelect={setDateRange}
                          numberOfMonths={1}
                        />
                      </div>
                    </PopoverContent>
                  </Popover>
                  <Select onValueChange={setCategoryFilter} defaultValue="all">
                    <SelectTrigger className='w-full sm:w-48'>
                      <SelectValue placeholder="Filter by category" />
                    </SelectTrigger>
                    <SelectContent>
                      {expenseCategories.map(category => (
                        <SelectItem key={category} value={category} className="capitalize">{category === 'all' ? t('project_expenses.all_categories') : category}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <div className="flex items-center space-x-2 w-full sm:w-auto">
                    <Switch
                      id="user-filter-toggle"
                      checked={userFilter === 'me'}
                      onCheckedChange={(checked) => setUserFilter(checked ? 'me' : 'all')}
                    />
                    <Label htmlFor="user-filter-toggle" className="text-sm font-medium whitespace-nowrap">
                      {userFilter === 'me' ? 'My Expenses' : 'All Expenses'}
                    </Label>
                  </div>
                  <Button variant="outline" size="sm" onClick={handleExportPDF} disabled={filteredExpenses.length === 0} className="w-full sm:w-auto">
                    <FileDown className="mr-2 h-4 w-4" />
                    {t('common.export')}
                  </Button>
                </div>
              </div>
            </div>

            {isLoading && (
              <div className="space-y-4">
                {[...Array(3)].map((_, i) => <Skeleton key={i} className="h-24" />)}
              </div>
            )}

            {!isLoading && filteredExpenses.length > 0 && (
              <div className="space-y-4">
                {filteredExpenses.map(expense => (
                  <Card key={expense.id}>
                    <CardContent className="p-4 flex justify-between items-start">
                      <div className="flex-1 flex gap-4 items-start">
                        <div className='flex items-center justify-center bg-muted rounded-full h-10 w-10 shrink-0 mt-1'>
                          {getCategoryIcon(expense.category)}
                        </div>
                        <div className="flex-1 space-y-1">
                          <p className="font-semibold">{expense.description}</p>
                          <Badge variant="secondary" className="capitalize">{expense.category}</Badge>
                          <div className="flex items-center flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground pt-1">
                            <div className="flex items-center gap-1.5">
                              <CalendarIcon className="h-3.5 w-3.5" />
                              <span>{formatDate(new Date())}</span>
                            </div>
                            <div className="flex items-center gap-1.5">
                              <User className="h-3.5 w-3.5" />
                              <span>{expense.user?.name || 'Unknown'}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                      <div className="text-right flex flex-col items-end ml-4">
                        <p className="font-bold text-lg mb-2">{formatCurrency(expense.amount)}</p>
                        {(userProfile?.role === 'admin' || expense.userId === user?.id) && (
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" className="w-8 h-8 p-0">
                                <MoreHorizontal className="w-4 h-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem onClick={() => handleEdit(expense)}>{t('common.edit')}</DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleDeleteRequest(expense)} className="text-red-600">{t('common.delete')}</DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}

            {!isLoading && filteredExpenses.length === 0 && (
              <div>
                <Card className="flex flex-col items-center justify-center h-64 text-center p-6 bg-card/80 border-2 border-dashed">
                  <h3 className="text-xl font-bold font-headline">{t('project_expenses.no_expenses')}</h3>
                  <p className="max-w-sm mt-2 text-muted-foreground">
                    {expenses && expenses.length > 0 ? 'No expenses match your current filters.' : 'No expenses logged yet. Click "Log Expense" to add one.'}
                  </p>
                </Card>
              </div>
            )}
          </div>
        </main>
      </div>

      <ExpenseFormSheet
        isOpen={isSheetOpen}
        setIsOpen={setIsSheetOpen}
        expense={editingExpense}
        projectId={projectId as string}
        onSuccess={handleExpenseUpdate}
      />

      <AlertDialog open={isDeleteAlertOpen} onOpenChange={setIsDeleteAlertOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the expense record.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common.cancel')}</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteConfirm} className="bg-destructive hover:bg-destructive/90">{t('common.delete')}</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}

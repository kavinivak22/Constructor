
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter
} from '@/components/ui/dialog';
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
  const { projectId } = useParams();
  const router = useRouter();
  const { toast } = useToast();
  const { supabase, user } = useSupabase();

  const [isSheetOpen, setIsSheetOpen] = useState(false);
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null);
  const [viewingExpense, setViewingExpense] = useState<Expense | null>(null);
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
        if (userData) {
          setUserProfile({
            ...userData,
            companyId: userData.company_id,
            displayName: userData.display_name,
            photoURL: userData.photo_url,
          } as AppUser);
        }

        // Fetch expenses with user info
        const { data: expensesData, error: expensesError } = await supabase
          .from('expenses')
          .select(`
            *,
            user:user_id (
              displayName:display_name,
              photoURL:photo_url
            )
          `)
          .eq('project_id', projectId)
          .order('expense_date', { ascending: false });

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

      // User filter - check both old and new column names
      const matchesUser = userFilter === 'all' || expense.user_id === user?.id || expense.userId === user?.id;

      // Date range filter
      let matchesDate = true;
      if (dateRange?.from && dateRange?.to) {
        const expenseDateStr = (expense.expense_date || expense.expenseDate) as string;
        const expenseDate = new Date(expenseDateStr);
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
            user:user_id (
              displayName:display_name,
              photoURL:photo_url
            )
          `)
          .eq('project_id', projectId)
          .order('expense_date', { ascending: false });

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
      <div className="flex flex-col h-full bg-transparent">
        <header className="flex items-center gap-2 sm:gap-3 px-3 sm:px-6 py-2 shrink-0 glass sticky top-0 z-10 border-b border-white/10 h-11 sm:h-14">
          <Button variant="ghost" size="icon" className="h-7 w-7 sm:h-8 sm:w-8 shrink-0" onClick={() => router.back()}>
            <ArrowLeft className="w-4 h-4" />
          </Button>
          <div className="flex-1 min-w-0">
            <h1 className="text-xs sm:text-lg font-bold tracking-tight font-headline truncate leading-none">
              Project Expenses
            </h1>
            <p className="text-[10px] sm:text-xs text-muted-foreground truncate leading-none mt-0.5">{project?.name || 'Loading...'}</p>
          </div>
          <Button size="sm" className="h-7 sm:h-8 text-xs font-semibold px-2.5 sm:px-3" onClick={handleAddNew}>
            <PlusCircle className="w-3.5 h-3.5 mr-1" />
            Log Expense
          </Button>
        </header>

        <main className="flex-1 p-4 overflow-y-auto md:p-6">
          <div className='max-w-4xl mx-auto'>
            <div className="mb-4 space-y-3">
              {/* Option 2: 3-Stat Financial Header Banner */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Card className="glass-card">
                  <CardContent className="p-3.5 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-primary/10 text-primary shrink-0">
                        <IndianRupee className="w-5 h-5" />
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground font-medium">Cash Spent (Outflows)</p>
                        <p className="text-lg font-bold tracking-tight text-foreground">{formatCurrency(totalExpenses)}</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card 
                  onClick={() => router.push(`/projects/${projectId}/expenses/consumption`)} 
                  className="glass-card cursor-pointer hover:border-amber-500/40 transition-all group"
                >
                  <CardContent className="p-3.5 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-amber-500/10 text-amber-500 shrink-0">
                        <Package className="w-5 h-5" />
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground font-medium flex items-center gap-1">
                          <span>Materials Consumed</span>
                          <span className="text-[10px] text-amber-500 font-bold group-hover:translate-x-0.5 transition-transform">➔</span>
                        </p>
                        <p className="text-xs text-amber-500 font-semibold mt-0.5">View Site Consumption Log →</p>
                      </div>
                    </div>
                    <Badge variant="outline" className="bg-amber-500/10 text-amber-500 border-amber-500/20 text-[10px] font-bold px-2 py-0.5">
                      Log Analytics
                    </Badge>
                  </CardContent>
                </Card>
              </div>

              {/* Responsive Compact Filter Controls */}
              <div className="space-y-2.5 sm:space-y-4">
                {/* Search Input */}
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder="Search expenses..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-9 h-9 text-xs sm:text-sm bg-background/50 border-white/10"
                  />
                </div>

                {/* Filter Grid: 2-column grid on mobile, inline flex row on desktop */}
                <div className="grid grid-cols-2 sm:flex sm:flex-wrap items-center gap-2 sm:gap-3">
                  {/* Date Filter */}
                  <Popover>
                    <PopoverTrigger asChild>
                      <Button
                        id="date"
                        variant="outline"
                        size="sm"
                        className={cn(
                          "w-full sm:w-[220px] justify-start text-left font-normal text-xs h-9 bg-background/50 border-white/10 truncate",
                          !dateRange && "text-muted-foreground"
                        )}
                      >
                        <CalendarDays className="mr-1.5 h-3.5 w-3.5 shrink-0" />
                        <span className="truncate">
                          {dateRange?.from ? (
                            dateRange.to ? (
                              `${format(dateRange.from, "MMM dd")} - ${format(dateRange.to, "MMM dd")}`
                            ) : (
                              format(dateRange.from, "MMM dd")
                            )
                          ) : (
                            "Filter date..."
                          )}
                        </span>
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-[92vw] sm:w-auto p-0 max-h-[85vh] overflow-y-auto border-white/10 glass-card z-50" align="start" side="bottom" sideOffset={4}>
                      <div className="flex flex-col sm:flex-row">
                        <div className="p-2 border-b sm:border-b-0 sm:border-r border-white/10 flex flex-row sm:flex-col flex-wrap gap-1 items-center sm:items-start overflow-x-auto shrink-0">
                          <Button variant="ghost" size="sm" className="text-xs h-7 sm:h-8 px-2 sm:w-full justify-start" onClick={() => setDateRange({ from: startOfDay(new Date()), to: endOfDay(new Date()) })}>Today</Button>
                          <Button variant="ghost" size="sm" className="text-xs h-7 sm:h-8 px-2 sm:w-full justify-start" onClick={() => setDateRange({ from: startOfDay(subDays(new Date(), 1)), to: endOfDay(subDays(new Date(), 1)) })}>Yesterday</Button>
                          <Button variant="ghost" size="sm" className="text-xs h-7 sm:h-8 px-2 sm:w-full justify-start" onClick={() => setDateRange({ from: startOfDay(subDays(new Date(), 6)), to: endOfDay(new Date()) })}>Last 7 days</Button>
                          <Button variant="ghost" size="sm" className="text-xs h-7 sm:h-8 px-2 sm:w-full justify-start" onClick={() => setDateRange({ from: startOfMonth(new Date()), to: endOfMonth(new Date()) })}>This Month</Button>
                          <Button variant="ghost" size="sm" className="text-xs h-7 sm:h-8 px-2 sm:w-full justify-start" onClick={() => setDateRange({ from: startOfMonth(subDays(new Date(), 30)), to: endOfMonth(subDays(new Date(), 30)) })}>Last Month</Button>
                          <Button variant="ghost" size="sm" className="text-xs h-7 sm:h-8 px-2 sm:w-full justify-start text-red-400 hover:text-red-300" onClick={() => setDateRange(undefined)}>Clear</Button>
                        </div>
                        <div className="p-1 sm:p-2 flex justify-center">
                          <Calendar
                            initialFocus
                            mode="range"
                            defaultMonth={dateRange?.from}
                            selected={dateRange}
                            onSelect={setDateRange}
                            numberOfMonths={1}
                            className="p-1 text-xs"
                          />
                        </div>
                      </div>
                    </PopoverContent>
                  </Popover>

                  {/* Category Select */}
                  <Select onValueChange={setCategoryFilter} defaultValue="all">
                    <SelectTrigger className="w-full sm:w-44 h-9 text-xs bg-background/50 border-white/10">
                      <SelectValue placeholder="Category" />
                    </SelectTrigger>
                    <SelectContent>
                      {expenseCategories.map(category => (
                        <SelectItem key={category} value={category} className="capitalize text-xs">
                          {category === 'all' ? 'All Categories' : category}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>

                  {/* My/All Toggle */}
                  <div className="col-span-1 flex items-center justify-between sm:justify-start gap-2 h-9 px-3 rounded-lg border border-white/10 bg-background/50">
                    <Switch
                      id="user-filter-toggle"
                      checked={userFilter === 'me'}
                      onCheckedChange={(checked) => setUserFilter(checked ? 'me' : 'all')}
                      className="scale-90"
                    />
                    <Label htmlFor="user-filter-toggle" className="text-xs font-medium whitespace-nowrap cursor-pointer">
                      {userFilter === 'me' ? 'My Expenses' : 'All Expenses'}
                    </Label>
                  </div>

                  {/* Export Button */}
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleExportPDF}
                    disabled={filteredExpenses.length === 0}
                    className="col-span-1 h-9 text-xs bg-background/50 border-white/10"
                  >
                    <FileDown className="mr-1.5 h-3.5 w-3.5" />
                    Export
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
              <div className="space-y-3">
                {filteredExpenses.map(expense => {
                  // Helper to clean up verbose Payout descriptions for clean, crisp rendering
                  const rawDesc = expense.description || 'Expense';
                  const cleanDesc = rawDesc
                    .replace(/Contractor Wages:\s*/gi, '')
                    .replace(/\(Mason Coolie\)/gi, 'Mason')
                    .replace(/\(Female Coolie\)/gi, 'Female Coolie');

                  return (
                    <Card 
                      key={expense.id} 
                      onClick={() => setViewingExpense(expense)}
                      className="glass-card hover:border-white/30 transition-all rounded-xl sm:rounded-2xl cursor-pointer group hover:shadow-md"
                    >
                      <CardContent className="p-3 sm:p-4 flex items-start justify-between gap-2.5 sm:gap-3">
                        <div className="flex items-start gap-2.5 sm:gap-3 min-w-0 flex-1">
                          <div className="flex items-center justify-center bg-primary/10 border border-primary/20 rounded-xl h-8 w-8 sm:h-10 sm:w-10 shrink-0 text-primary mt-0.5 group-hover:scale-105 transition-transform">
                            {getCategoryIcon(expense.category)}
                          </div>
                          <div className="min-w-0 flex-1 space-y-1">
                            <h4 className="font-bold text-xs sm:text-sm text-foreground line-clamp-2 leading-snug font-headline group-hover:text-primary transition-colors">
                              {cleanDesc}
                            </h4>
                            <div className="flex items-center flex-wrap gap-x-2.5 gap-y-1 text-[11px] sm:text-xs text-muted-foreground pt-0.5">
                              <Badge variant="secondary" className="capitalize text-[10px] px-1.5 py-0 h-4 shrink-0 bg-white/5 border-white/10">
                                {expense.category}
                              </Badge>
                              <div className="flex items-center gap-1 shrink-0">
                                <CalendarIcon className="h-3 w-3 text-muted-foreground/70" />
                                <span>{formatDate(new Date((expense.expense_date || expense.expenseDate) as string))}</span>
                              </div>
                              <span className="text-white/20 hidden sm:inline">•</span>
                              <div className="flex items-center gap-1 shrink-0">
                                <User className="h-3 w-3 text-muted-foreground/70" />
                                <span className="truncate max-w-[100px] sm:max-w-[140px]">{expense.user?.displayName || expense.user?.name || 'Unknown'}</span>
                              </div>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-1 sm:gap-2 shrink-0 ml-1">
                          <span className="font-bold text-xs sm:text-base text-foreground tracking-tight">
                            {formatCurrency(expense.amount)}
                          </span>
                          {(userProfile?.role === 'admin' || expense.user_id === user?.id || expense.userId === user?.id) && (
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                                <Button variant="ghost" size="icon" className="h-7 w-7 sm:h-8 sm:w-8 text-muted-foreground hover:text-foreground">
                                  <MoreHorizontal className="w-4 h-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="glass-card border-white/10">
                                <DropdownMenuItem onClick={(e) => { e.stopPropagation(); handleEdit(expense); }} className="text-xs">Edit</DropdownMenuItem>
                                <DropdownMenuItem onClick={(e) => { e.stopPropagation(); handleDeleteRequest(expense); }} className="text-xs text-red-500">Delete</DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            )}

            {!isLoading && filteredExpenses.length === 0 && (
              <div>
                <Card className="glass-card flex flex-col items-center justify-center h-64 text-center p-6 border-2 border-dashed border-white/20 dark:border-white/10">
                  <h3 className="text-xl font-bold font-headline">No Expenses Found</h3>
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

      {/* Expense Details Dialog */}
      <Dialog open={!!viewingExpense} onOpenChange={(open) => !open && setViewingExpense(null)}>
        <DialogContent className="sm:max-w-md glass-card border-white/10 p-5 rounded-2xl">
          {viewingExpense && (
            <div className="space-y-4">
              <DialogHeader className="space-y-1.5 text-left">
                <div className="flex items-center justify-between gap-2 pr-6">
                  <Badge variant="secondary" className="capitalize text-xs px-2.5 py-0.5 bg-primary/10 text-primary border-primary/20 font-semibold">
                    {viewingExpense.category}
                  </Badge>
                  <span className="text-xs text-muted-foreground font-medium">
                    {formatDate(new Date((viewingExpense.expense_date || viewingExpense.expenseDate) as string))}
                  </span>
                </div>
                <DialogTitle className="text-base sm:text-lg font-bold font-headline pt-1 text-foreground leading-snug">
                  {viewingExpense.description}
                </DialogTitle>
              </DialogHeader>

              {/* Amount Banner */}
              <div className="p-3.5 sm:p-4 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-between">
                <div>
                  <p className="text-xs text-muted-foreground font-medium">Expense Amount</p>
                  <p className="text-xl sm:text-2xl font-extrabold text-primary font-headline mt-0.5">
                    {formatCurrency(viewingExpense.amount)}
                  </p>
                </div>
                <div className="w-10 h-10 rounded-xl bg-primary/20 flex items-center justify-center text-primary shrink-0">
                  <IndianRupee className="w-5 h-5" />
                </div>
              </div>

              {/* Details Grid */}
              <div className="grid grid-cols-2 gap-3 text-xs p-3.5 rounded-xl bg-background/50 border border-white/10">
                <div>
                  <p className="text-muted-foreground font-medium text-[11px]">Logged By</p>
                  <p className="font-semibold text-foreground mt-0.5 truncate">
                    {viewingExpense.user?.displayName || viewingExpense.user?.name || 'Site Manager'}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground font-medium text-[11px]">Category</p>
                  <p className="font-semibold text-foreground capitalize mt-0.5">
                    {viewingExpense.category}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground font-medium text-[11px]">Project Site</p>
                  <p className="font-semibold text-foreground mt-0.5 truncate">
                    {project?.name || 'Current Site'}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground font-medium text-[11px]">Status</p>
                  <Badge variant="outline" className="text-[10px] bg-emerald-500/10 text-emerald-400 border-emerald-500/30 font-semibold mt-0.5 px-2 py-0">
                    {(viewingExpense.payment_status || 'Paid').toUpperCase()}
                  </Badge>
                </div>
              </div>

              {/* Notes & References */}
              {viewingExpense.notes && (
                <div className="space-y-1 text-xs">
                  <p className="text-muted-foreground font-medium text-[11px]">Notes & Reference Details</p>
                  <div className="p-3 rounded-xl bg-background/30 border border-white/5 text-muted-foreground leading-relaxed whitespace-pre-wrap max-h-32 overflow-y-auto text-[11px]">
                    {viewingExpense.notes}
                  </div>
                </div>
              )}

              {/* Receipt attachment if available */}
              {(viewingExpense.receipt_url || viewingExpense.receiptUrl) && (
                <div className="space-y-1 text-xs pt-1">
                  <p className="text-muted-foreground font-medium text-[11px]">Receipt Document</p>
                  <a 
                    href={viewingExpense.receipt_url || viewingExpense.receiptUrl} 
                    target="_blank" 
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 text-xs text-primary hover:underline font-semibold"
                  >
                    View Uploaded Receipt
                  </a>
                </div>
              )}

              {/* Footer Actions */}
              <DialogFooter className="flex flex-row justify-end gap-2 pt-2 border-t border-white/10">
                {(userProfile?.role === 'admin' || viewingExpense.user_id === user?.id || viewingExpense.userId === user?.id) && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      const exp = viewingExpense;
                      setViewingExpense(null);
                      handleEdit(exp);
                    }}
                    className="text-xs border-white/10 h-8"
                  >
                    Edit Expense
                  </Button>
                )}
                <Button size="sm" onClick={() => setViewingExpense(null)} className="text-xs h-8">
                  Close
                </Button>
              </DialogFooter>
            </div>
          )}
        </DialogContent>
      </Dialog>

      <AlertDialog open={isDeleteAlertOpen} onOpenChange={setIsDeleteAlertOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the expense record.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteConfirm} className="bg-destructive hover:bg-destructive/90">Delete</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}

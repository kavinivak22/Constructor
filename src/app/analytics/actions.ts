'use server';

import { createClient } from '@/utils/supabase/server';

export interface ProjectCostMetric {
  projectName: string;
  budget: number;
  actualCost: number;
}

export interface ExpenseCategoryMetric {
  category: string;
  amount: number;
}

export interface LaborActivityMetric {
  date: string;
  workerCount: number;
  [workerType: string]: any; // E.g. Mason, Laborer, etc.
}

export interface AnalyticsSummary {
  totalBudget: number;
  totalActualCost: number;
  totalExpenses: number;
  totalPurchaseOrders: number;
  projectCosts: ProjectCostMetric[];
  expenseCategories: ExpenseCategoryMetric[];
  laborActivity: LaborActivityMetric[];
}

export async function getAnalyticsData(): Promise<AnalyticsSummary> {
  const supabase = await createClient();

  const { data: { user } } = await supabase.auth.getUser();
  if (!user) throw new Error('Not authenticated');

  // 1. Get user profile and companyId
  const { data: userData, error: userError } = await supabase
    .from('users')
    .select('companyId')
    .eq('id', user.id)
    .single();

  if (userError || !userData?.companyId) {
    return {
      totalBudget: 0,
      totalActualCost: 0,
      totalExpenses: 0,
      totalPurchaseOrders: 0,
      projectCosts: [],
      expenseCategories: [],
      laborActivity: []
    };
  }

  const companyId = userData.companyId;

  // 2. Fetch projects
  const { data: projects, error: projectsError } = await supabase
    .from('projects')
    .select('id, name, budget')
    .eq('companyId', companyId);

  if (projectsError || !projects || projects.length === 0) {
    return {
      totalBudget: 0,
      totalActualCost: 0,
      totalExpenses: 0,
      totalPurchaseOrders: 0,
      projectCosts: [],
      expenseCategories: [],
      laborActivity: []
    };
  }

  const projectIds = projects.map(p => p.id);

  // 3. Fetch expenses
  const { data: expenses, error: expensesError } = await supabase
    .from('expenses')
    .select('project_id, amount, category, expense_date')
    .in('project_id', projectIds);

  const safeExpenses = expenses || [];

  // 4. Fetch purchase orders
  const { data: purchaseOrders, error: poError } = await supabase
    .from('purchase_orders')
    .select('project_id, total_amount, status')
    .in('project_id', projectIds);

  const safePO = purchaseOrders || [];

  // 5. Fetch daily worklogs + labor counts
  const { data: dailyWorklogs, error: worklogError } = await supabase
    .from('daily_worklogs')
    .select(`
      date,
      labor:worklog_labor_entries (
        workers:worklog_worker_counts (
          worker_type,
          count
        )
      )
    `)
    .in('project_id', projectIds)
    .order('date', { ascending: true });

  const safeWorklogs = dailyWorklogs || [];

  // --- Calculate Metrics ---

  // Aggregate project cost details
  const projectCosts: ProjectCostMetric[] = projects.map(p => {
    const projectExpenses = safeExpenses
      .filter(e => e.project_id === p.id)
      .reduce((sum, e) => sum + Number(e.amount || 0), 0);

    const projectPOs = safePO
      .filter(po => po.project_id === p.id)
      .reduce((sum, po) => sum + Number(po.total_amount || 0), 0);

    return {
      projectName: p.name,
      budget: Number(p.budget || 0),
      actualCost: projectExpenses + projectPOs
    };
  });

  // Aggregate expense categories
  const categoryMap: { [cat: string]: number } = {};
  safeExpenses.forEach(e => {
    const cat = e.category || 'Other';
    categoryMap[cat] = (categoryMap[cat] || 0) + Number(e.amount || 0);
  });
  const expenseCategories: ExpenseCategoryMetric[] = Object.entries(categoryMap).map(
    ([category, amount]) => ({ category, amount })
  );

  // Aggregate labor logs by date
  const laborActivityMap: { [date: string]: { [type: string]: number } } = {};
  safeWorklogs.forEach(log => {
    const dateStr = log.date;
    if (!laborActivityMap[dateStr]) {
      laborActivityMap[dateStr] = {};
    }

    if (log.labor) {
      log.labor.forEach((entry: any) => {
        if (entry.workers) {
          entry.workers.forEach((w: any) => {
            const type = w.worker_type || 'General';
            const count = Number(w.count || 0);
            laborActivityMap[dateStr][type] = (laborActivityMap[dateStr][type] || 0) + count;
          });
        }
      });
    }
  });

  const laborActivity: LaborActivityMetric[] = Object.entries(laborActivityMap).map(
    ([date, workerTypes]) => {
      const totalCount = Object.values(workerTypes).reduce((sum, count) => sum + count, 0);
      return {
        date,
        workerCount: totalCount,
        ...workerTypes
      };
    }
  );

  const totalBudget = projectCosts.reduce((sum, p) => sum + p.budget, 0);
  const totalActualCost = projectCosts.reduce((sum, p) => sum + p.actualCost, 0);
  const totalExpenses = safeExpenses.reduce((sum, e) => sum + Number(e.amount || 0), 0);
  const totalPurchaseOrders = safePO.reduce((sum, po) => sum + Number(po.total_amount || 0), 0);

  return {
    totalBudget,
    totalActualCost,
    totalExpenses,
    totalPurchaseOrders,
    projectCosts,
    expenseCategories,
    laborActivity
  };
}

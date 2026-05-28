'use client';

import { useEffect, useState, startTransition } from 'react';
import { PlaceholderPage } from "@/components/placeholder-page";
import { AreaChart, TrendingUp, TrendingDown, IndianRupee, Users, Package, AlertCircle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Loader2 } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import {
  BarChart as RechartsBarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart as RechartsPieChart,
  Pie,
  Cell,
  AreaChart as RechartsAreaChart,
  Area
} from "recharts";
import { getAnalyticsData, type AnalyticsSummary } from "./actions";

// Premium HSL colors
const COLORS = [
  "hsl(246, 80%, 60%)", // Indigo
  "hsl(142, 72%, 40%)", // Emerald
  "hsl(24, 94%, 50%)",  // Orange/Amber
  "hsl(339, 90%, 55%)", // Rose
  "hsl(199, 89%, 48%)", // Sky Blue
  "hsl(271, 91%, 65%)", // Purple
];

const DEMO_DATA: AnalyticsSummary = {
  totalBudget: 12500000,
  totalActualCost: 8450000,
  totalExpenses: 3450000,
  totalPurchaseOrders: 5000000,
  projectCosts: [
    { projectName: "Commercial Plaza", budget: 6000000, actualCost: 4500000 },
    { projectName: "Oak Residential", budget: 4500000, actualCost: 3200000 },
    { projectName: "City Park Renovation", budget: 2000000, actualCost: 750000 },
  ],
  expenseCategories: [
    { category: "Materials", amount: 3450000 },
    { category: "Labor", amount: 2800000 },
    { category: "Equipment Rental", amount: 1200000 },
    { category: "Permits & Legal", amount: 650000 },
    { category: "Subcontractors", amount: 350000 },
  ],
  laborActivity: [
    { date: "2026-05-15", workerCount: 15, Mason: 5, Laborer: 8, Operator: 2 },
    { date: "2026-05-16", workerCount: 18, Mason: 6, Laborer: 10, Operator: 2 },
    { date: "2026-05-17", workerCount: 22, Mason: 8, Laborer: 11, Operator: 3 },
    { date: "2026-05-18", workerCount: 20, Mason: 7, Laborer: 10, Operator: 3 },
    { date: "2026-05-19", workerCount: 25, Mason: 9, Laborer: 12, Operator: 4 },
    { date: "2026-05-20", workerCount: 27, Mason: 10, Laborer: 13, Operator: 4 },
    { date: "2026-05-21", workerCount: 30, Mason: 11, Laborer: 15, Operator: 4 },
  ],
};

export default function AnalyticsPage() {
  const [data, setData] = useState<AnalyticsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [useDemoData, setUseDemoData] = useState(false);
  const [hasNoData, setHasNoData] = useState(false);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      try {
        const result = await getAnalyticsData();
        // Check if DB is effectively empty
        if (result.projectCosts.length === 0 && result.expenseCategories.length === 0) {
          setHasNoData(true);
          setUseDemoData(true); // Automatically fall back to demo data to keep the UI premium
          setData(DEMO_DATA);
        } else {
          setHasNoData(false);
          setData(result);
        }
      } catch (error) {
        console.error("Failed to load analytics data:", error);
        setHasNoData(true);
        setUseDemoData(true);
        setData(DEMO_DATA);
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, []);

  const handleToggleDemo = (checked: boolean) => {
    setUseDemoData(checked);
    if (checked) {
      setData(DEMO_DATA);
    } else {
      setLoading(true);
      getAnalyticsData()
        .then((result) => {
          setData(result);
          setLoading(false);
        })
        .catch(() => {
          setData(DEMO_DATA);
          setLoading(false);
        });
    }
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-muted-foreground animate-pulse">Aggregating project financials...</p>
        </div>
      </div>
    );
  }

  const activeData = data || DEMO_DATA;
  const isBudgetExceeded = activeData.totalActualCost > activeData.totalBudget;
  const budgetRatio = activeData.totalBudget > 0 ? (activeData.totalActualCost / activeData.totalBudget) * 100 : 0;

  // Custom tooltips for graphs
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(value);
  };

  return (
    <PlaceholderPage
      title="Analytics Dashboard"
      description="Real-time financial tracking, budget performance metrics, and labor allocations."
      icon={AreaChart}
    >
      <div className="space-y-6">
        {/* Top Controls & Alerts */}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 p-4 rounded-xl glass shadow-sm">
          <div className="space-y-1">
            <h3 className="font-semibold text-sm">Data Source Configuration</h3>
            <p className="text-xs text-muted-foreground">
              {useDemoData 
                ? "Showing simulated premium workspace data for preview purposes."
                : "Showing live calculations direct from your project logs and databases."}
            </p>
          </div>
          <div className="flex items-center space-x-2">
            <Switch
              id="demo-mode"
              checked={useDemoData}
              onCheckedChange={handleToggleDemo}
            />
            <Label htmlFor="demo-mode" className="text-xs font-semibold cursor-pointer">
              Show Demo Metrics {hasNoData && <span className="text-amber-500 font-normal">(Default: No Live Data)</span>}
            </Label>
          </div>
        </div>

        {hasNoData && !useDemoData && (
          <Alert variant="default" className="border-amber-500/50 bg-amber-500/5">
            <AlertCircle className="h-4 w-4 text-amber-500" />
            <AlertTitle className="text-amber-500 font-semibold">No active projects found</AlertTitle>
            <AlertDescription>
              We couldn't locate any projects or expenses linked to your company. Toggle "Show Demo Metrics" above to preview how this dashboard handles financial data.
            </AlertDescription>
          </Alert>
        )}

        {/* Financial KPI Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="glass-card overflow-hidden relative">
            <div className="absolute top-0 left-0 w-full h-[3px] bg-indigo-500" />
            <CardHeader className="pb-2">
              <CardDescription className="text-xs font-medium uppercase tracking-wider">Total Portfolio Budget</CardDescription>
              <CardTitle className="text-2xl font-black font-headline tracking-tight">{formatCurrency(activeData.totalBudget)}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center text-xs text-muted-foreground gap-1">
                <IndianRupee className="h-3 w-3 text-indigo-500" />
                <span>Aggregated project allocations</span>
              </div>
            </CardContent>
          </Card>

          <Card className="glass-card overflow-hidden relative">
            <div className="absolute top-0 left-0 w-full h-[3px] bg-emerald-500" />
            <CardHeader className="pb-2">
              <CardDescription className="text-xs font-medium uppercase tracking-wider">Total Actual Expenses</CardDescription>
              <CardTitle className="text-2xl font-black font-headline tracking-tight">{formatCurrency(activeData.totalActualCost)}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center text-xs gap-1">
                {isBudgetExceeded ? (
                  <span className="text-red-500 flex items-center gap-0.5">
                    <TrendingUp className="h-3 w-3" />
                    Exceeds budget ({budgetRatio.toFixed(1)}%)
                  </span>
                ) : (
                  <span className="text-emerald-500 flex items-center gap-0.5">
                    <TrendingDown className="h-3 w-3" />
                    Within budget ({budgetRatio.toFixed(1)}%)
                  </span>
                )}
              </div>
            </CardContent>
          </Card>

          <Card className="glass-card overflow-hidden relative">
            <div className="absolute top-0 left-0 w-full h-[3px] bg-amber-500" />
            <CardHeader className="pb-2">
              <CardDescription className="text-xs font-medium uppercase tracking-wider">Direct Cash Expenses</CardDescription>
              <CardTitle className="text-2xl font-black font-headline tracking-tight">{formatCurrency(activeData.totalExpenses)}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center text-xs text-muted-foreground gap-1">
                <TrendingUp className="h-3 w-3 text-amber-500" />
                <span>Petty cash & receipts logged</span>
              </div>
            </CardContent>
          </Card>

          <Card className="glass-card overflow-hidden relative">
            <div className="absolute top-0 left-0 w-full h-[3px] bg-rose-500" />
            <CardHeader className="pb-2">
              <CardDescription className="text-xs font-medium uppercase tracking-wider">Purchase Orders (PO)</CardDescription>
              <CardTitle className="text-2xl font-black font-headline tracking-tight">{formatCurrency(activeData.totalPurchaseOrders)}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center text-xs text-muted-foreground gap-1">
                <Package className="h-3 w-3 text-rose-500" />
                <span>Approved material contracts</span>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Charts Section */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Budget vs Actual Bar Chart */}
          <Card className="glass-card lg:col-span-2 shadow-sm">
            <CardHeader>
              <CardTitle className="text-lg font-bold font-headline">Budget vs. Actual Costs</CardTitle>
              <CardDescription>Comparison of target budgets and combined actual expenditures per project</CardDescription>
            </CardHeader>
            <CardContent>
              {activeData.projectCosts.length === 0 ? (
                <div className="flex h-[300px] items-center justify-center text-muted-foreground text-sm border border-dashed rounded-lg">
                  No project cost comparisons available.
                </div>
              ) : (
                <div className="h-[300px] w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <RechartsBarChart
                      data={activeData.projectCosts}
                      margin={{ top: 10, right: 10, left: 10, bottom: 20 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="projectName" tickLine={false} axisLine={false} tick={{ fontSize: 11 }} />
                      <YAxis
                        tickLine={false}
                        axisLine={false}
                        tickFormatter={(v) => `$${v / 1000}k`}
                        tick={{ fontSize: 11 }}
                      />
                      <Tooltip
                        formatter={(value: any) => [formatCurrency(value), '']}
                        contentStyle={{
                          background: 'rgba(255, 255, 255, 0.95)',
                          borderRadius: '8px',
                          border: '1px solid #e2e8f0',
                        }}
                      />
                      <Legend wrapperStyle={{ fontSize: 12, paddingTop: 10 }} />
                      <Bar dataKey="budget" name="Allocated Budget" fill="hsl(246, 80%, 60%)" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="actualCost" name="Actual Cost" fill="hsl(142, 72%, 40%)" radius={[4, 4, 0, 0]} />
                    </RechartsBarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Expense Category Breakdown Pie Chart */}
          <Card className="glass-card shadow-sm">
            <CardHeader>
              <CardTitle className="text-lg font-bold font-headline">Expense Breakdown</CardTitle>
              <CardDescription>Distribution of project payments across categories</CardDescription>
            </CardHeader>
            <CardContent>
              {activeData.expenseCategories.length === 0 ? (
                <div className="flex h-[300px] items-center justify-center text-muted-foreground text-sm border border-dashed rounded-lg">
                  No category distributions available.
                </div>
              ) : (
                <div className="h-[300px] w-full flex flex-col justify-between">
                  <div className="h-[220px] w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <RechartsPieChart>
                        <Pie
                          data={activeData.expenseCategories}
                          cx="50%"
                          cy="50%"
                          innerRadius={60}
                          outerRadius={80}
                          paddingAngle={3}
                          dataKey="amount"
                          nameKey="category"
                        >
                          {activeData.expenseCategories.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip
                          formatter={(value: any) => [formatCurrency(value), '']}
                          contentStyle={{
                            background: 'rgba(255, 255, 255, 0.95)',
                            borderRadius: '8px',
                            border: '1px solid #e2e8f0',
                          }}
                        />
                      </RechartsPieChart>
                    </ResponsiveContainer>
                  </div>
                  <div className="grid grid-cols-2 gap-x-2 gap-y-1 text-xs px-2 pt-2 border-t">
                    {activeData.expenseCategories.map((item, index) => (
                      <div key={item.category} className="flex items-center gap-1.5 min-w-0">
                        <span
                          className="h-2.5 w-2.5 rounded-full shrink-0"
                          style={{ backgroundColor: COLORS[index % COLORS.length] }}
                        />
                        <span className="truncate text-muted-foreground">{item.category}</span>
                        <span className="ml-auto font-semibold shrink-0">
                          {formatCurrency(item.amount)}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Worker Allocations & Activity Area Chart */}
        <Card className="glass-card w-full shadow-sm">
          <CardHeader>
            <CardTitle className="text-lg font-bold font-headline flex items-center gap-2">
              <Users className="h-5 w-5 text-primary" />
              Labor Allocations & Activity
            </CardTitle>
            <CardDescription>Daily worker counts deployed across active project sites</CardDescription>
          </CardHeader>
          <CardContent>
            {activeData.laborActivity.length === 0 ? (
              <div className="flex h-[320px] items-center justify-center text-muted-foreground text-sm border border-dashed rounded-lg">
                No worker count histories logged in daily worklogs yet.
              </div>
            ) : (
              <div className="h-[320px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <RechartsAreaChart
                    data={activeData.laborActivity}
                    margin={{ top: 10, right: 10, left: 10, bottom: 10 }}
                  >
                    <defs>
                      <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="hsl(246, 80%, 60%)" stopOpacity={0.4}/>
                        <stop offset="95%" stopColor="hsl(246, 80%, 60%)" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="date" tickLine={false} axisLine={false} tick={{ fontSize: 11 }} />
                    <YAxis tickLine={false} axisLine={false} tick={{ fontSize: 11 }} />
                    <Tooltip
                      contentStyle={{
                        background: 'rgba(255, 255, 255, 0.95)',
                        borderRadius: '8px',
                        border: '1px solid #e2e8f0',
                      }}
                    />
                    <Legend wrapperStyle={{ fontSize: 12, paddingTop: 10 }} />
                    <Area
                      type="monotone"
                      dataKey="workerCount"
                      name="Total Workers"
                      stroke="hsl(246, 80%, 60%)"
                      fillOpacity={1}
                      fill="url(#colorCount)"
                      strokeWidth={2}
                    />
                    {/* Render individual labor types if we have them */}
                    {Object.keys(activeData.laborActivity[0] || {})
                      .filter(key => key !== 'date' && key !== 'workerCount')
                      .map((workerType, index) => (
                        <Area
                          key={workerType}
                          type="monotone"
                          dataKey={workerType}
                          name={workerType}
                          stroke={COLORS[(index + 1) % COLORS.length]}
                          fill="transparent"
                          strokeWidth={1.5}
                        />
                      ))}
                  </RechartsAreaChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </PlaceholderPage>
  );
}


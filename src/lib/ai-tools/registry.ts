export interface AIToolDefinition {
  name: string;
  description: string;
  parameters: {
    type: string;
    properties: Record<string, { type: string; description: string; enum?: string[] }>;
    required?: string[];
  };
}

export interface StagedVoiceAction {
  id: string;
  toolName: string;
  summary: string;
  summaryTa: string;
  params: Record<string, any>;
  timestamp: string;
}

export const CONSTRUCTOR_AI_TOOLS: AIToolDefinition[] = [
  // 1. Query Contractor Payment & Balance
  {
    name: 'query_contractor_payments',
    description: 'Fetch payment history, total wages earned, NMR labor settlements, and current outstanding balance for a specific contractor or category.',
    parameters: {
      type: 'object',
      properties: {
        contractorName: { type: 'string', description: 'Name of the contractor or worker (e.g. Mani, Murugan, Ramesh, Mason)' },
        period: { type: 'string', description: 'Time period to query (e.g. this_week, last_week, this_month, all_time)', enum: ['this_week', 'last_week', 'this_month', 'all_time'] },
      },
      required: ['contractorName'],
    },
  },

  // 2. Query Material Stock & Reconciliation
  {
    name: 'query_material_stock',
    description: 'Fetch stock levels, consumption rates, unit prices, and budget drift for materials like Cement, Steel, Sand, AAC Blocks, Blue Metal.',
    parameters: {
      type: 'object',
      properties: {
        materialName: { type: 'string', description: 'Name of the material (e.g. Cement, TMT Steel, M-Sand, Red Bricks)' },
        projectName: { type: 'string', description: 'Optional project name filter' },
      },
    },
  },

  // 3. Query Project Financials & Budgets
  {
    name: 'query_project_financials',
    description: 'Fetch total budget, actual spend, client payment milestone collections, and net profit drift for a project.',
    parameters: {
      type: 'object',
      properties: {
        projectName: { type: 'string', description: 'Name of the project (e.g. Downtown Office, Coastal Villa, Suburban Bridge)' },
      },
    },
  },

  // 4. Query Daily Worklogs & Attendance
  {
    name: 'query_daily_worklogs',
    description: 'Fetch daily worklog entries, labor attendance, and site activities logged for today, yesterday, or this week.',
    parameters: {
      type: 'object',
      properties: {
        projectName: { type: 'string', description: 'Optional project name filter' },
        dateFilter: { type: 'string', description: 'Date to query (e.g. today, yesterday, this_week)', enum: ['today', 'yesterday', 'this_week'] },
      },
    },
  },

  // 5. Query Employees & Staff Roster
  {
    name: 'query_employees',
    description: 'Fetch employee list, site engineers, supervisors, roles, contact numbers, and project assignments.',
    parameters: {
      type: 'object',
      properties: {
        role: { type: 'string', description: 'Role filter (e.g. Admin, Manager, Engineer, Supervisor)' },
        name: { type: 'string', description: 'Employee name filter' },
      },
    },
  },

  // 6. Query Client Milestones & Stage Billing
  {
    name: 'query_client_milestones',
    description: 'Fetch stage-wise client payment milestones (Booking, Foundation, Slab, Plastering, Handover), collected amounts, and pending collections.',
    parameters: {
      type: 'object',
      properties: {
        projectName: { type: 'string', description: 'Project name filter' },
      },
    },
  },

  // 7. Query Purchase Orders
  {
    name: 'query_purchase_orders',
    description: 'Fetch purchase orders, material orders, supplier PO status (Draft, Sent, Approved, Delivered), and order values.',
    parameters: {
      type: 'object',
      properties: {
        supplierName: { type: 'string', description: 'Supplier name filter' },
        status: { type: 'string', description: 'PO status filter' },
      },
    },
  },

  // 8. Query Pouch Balances
  {
    name: 'query_pouch_balance',
    description: 'Fetch personal pouch cash balance, site supervisor petty cash floats, and recent pouch transactions.',
    parameters: {
      type: 'object',
      properties: {
        pouchType: { type: 'string', description: 'Personal vs Project Pouch', enum: ['personal', 'project'] },
      },
    },
  },

  // 9. Query Work Prep Tasks
  {
    name: 'query_work_prep_tasks',
    description: 'Fetch tomorrow work prep checklist, contractor preps, material dispatch checks, and site call lists.',
    parameters: {
      type: 'object',
      properties: {
        projectName: { type: 'string', description: 'Optional project filter' },
      },
    },
  },

  // 10. Query Site Analytics Summary
  {
    name: 'query_analytics_summary',
    description: 'Fetch overall site performance analytics, labor cost breakdowns, monthly expense metrics, and material drift trends.',
    parameters: {
      type: 'object',
      properties: {
        metric: { type: 'string', description: 'Metric type (e.g. labor, materials, expenses, overall)' },
      },
    },
  },

  // 11. Stage Daily Worklog Entry
  {
    name: 'stage_worklog_entry',
    description: 'Stage a daily site worklog entry containing labor attendance breakdown and materials consumed.',
    parameters: {
      type: 'object',
      properties: {
        projectName: { type: 'string', description: 'Name of the construction project or building site' },
        contractorName: { type: 'string', description: 'Contractor or team head name' },
        workerRole: { type: 'string', description: 'Role of workers (e.g. Mason, Male Helper, Female Helper, Electrician, Painter)' },
        workerCount: { type: 'number', description: 'Number of workers present' },
        dailyWage: { type: 'number', description: 'Daily wage rate per worker in INR (e.g. 800, 1000)' },
        workDescription: { type: 'string', description: 'Summary description of work done on site' },
        materialConsumed: { type: 'string', description: 'Optional material used (e.g. 20 bags Cement, 2 tons Steel)' },
      },
      required: ['workerRole', 'workerCount'],
    },
  },

  // 12. Stage Material Delivery Receipt
  {
    name: 'stage_material_receipt',
    description: 'Stage a material delivery receipt to update inventory stock and supplier PO.',
    parameters: {
      type: 'object',
      properties: {
        projectName: { type: 'string', description: 'Name of the construction site receiving materials' },
        materialName: { type: 'string', description: 'Name of material received (e.g. Cement, M-Sand, TMT Steel)' },
        quantity: { type: 'number', description: 'Quantity received' },
        unit: { type: 'string', description: 'Unit of measurement (e.g. bags, tons, loads, sqft, nos)' },
        totalCost: { type: 'number', description: 'Total cost in INR' },
        supplierName: { type: 'string', description: 'Supplier or vendor name' },
      },
      required: ['materialName', 'quantity'],
    },
  },

  // 13. Stage Contractor Payment
  {
    name: 'stage_contractor_payment',
    description: 'Stage a cash, UPI (GPay/PhonePe), or bank transfer payment to a contractor.',
    parameters: {
      type: 'object',
      properties: {
        contractorName: { type: 'string', description: 'Name of the contractor or labor lead receiving payment' },
        amount: { type: 'number', description: 'Payment amount in INR' },
        paymentMode: { type: 'string', description: 'Payment mode (e.g. UPI, Cash, Bank Transfer)', enum: ['UPI', 'Cash', 'Bank Transfer'] },
        projectName: { type: 'string', description: 'Project associated with payment' },
        notes: { type: 'string', description: 'Optional notes or remarks' },
      },
      required: ['contractorName', 'amount'],
    },
  },

  // 14. Stage Project Expense Voucher
  {
    name: 'stage_project_expense',
    description: 'Stage a petty cash or project expense voucher (e.g., fuel, site food, tea, small tools).',
    parameters: {
      type: 'object',
      properties: {
        projectName: { type: 'string', description: 'Name of the project' },
        category: { type: 'string', description: 'Expense category (e.g. Site Food, Fuel, Tools, Refreshments, Transport)' },
        amount: { type: 'number', description: 'Expense amount in INR' },
        description: { type: 'string', description: 'Details of expense' },
      },
      required: ['category', 'amount'],
    },
  },

  // 15. Stage Create Project
  {
    name: 'stage_create_project',
    description: 'Stage creation of a new construction project or building site.',
    parameters: {
      type: 'object',
      properties: {
        name: { type: 'string', description: 'Project name' },
        clientName: { type: 'string', description: 'Client or property owner name' },
        location: { type: 'string', description: 'Project location or address' },
        budget: { type: 'number', description: 'Total estimated budget in INR' },
      },
      required: ['name'],
    },
  },

  // 16. Stage Add Employee
  {
    name: 'stage_add_employee',
    description: 'Stage adding a new site engineer, supervisor, or staff member.',
    parameters: {
      type: 'object',
      properties: {
        displayName: { type: 'string', description: 'Full name of employee' },
        email: { type: 'string', description: 'Email address' },
        role: { type: 'string', description: 'Role (Admin, Manager, Engineer, Supervisor)' },
      },
      required: ['displayName'],
    },
  },

  // 17. Stage Record Client Payment
  {
    name: 'stage_record_client_payment',
    description: 'Stage recording payment received from project client for a milestone.',
    parameters: {
      type: 'object',
      properties: {
        projectName: { type: 'string', description: 'Project name' },
        milestoneStage: { type: 'string', description: 'Stage (e.g. Foundation, Slab, Plastering, Handover)' },
        amountReceived: { type: 'number', description: 'Amount collected in INR' },
      },
      required: ['projectName', 'amountReceived'],
    },
  },

  // 18. Stage Create Prep Task
  {
    name: 'stage_create_prep_task',
    description: 'Stage adding a task to tomorrow daily work prep board.',
    parameters: {
      type: 'object',
      properties: {
        projectName: { type: 'string', description: 'Project name' },
        taskTitle: { type: 'string', description: 'Preparation task description' },
        assigneeName: { type: 'string', description: 'Person assigned to complete call/task' },
      },
      required: ['taskTitle'],
    },
  },

  // 19. Navigation Tool
  {
    name: 'navigate_app_page',
    description: 'Navigate to any page in Constructor (e.g. Contractor Accounts, Weekly Pay-Day, Daily Worklog, Material Reconciliation, Client Milestones, Employees, Inventory).',
    parameters: {
      type: 'object',
      properties: {
        targetPage: { type: 'string', description: 'Target route (e.g. /financials/contractors, /worklog, /materials/reconciliation, /financials/payday, /projects, /employees, /inventory, /projects/milestones)' },
        pageName: { type: 'string', description: 'Display name of page' },
      },
      required: ['targetPage'],
    },
  },
];

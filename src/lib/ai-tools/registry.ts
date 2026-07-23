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

  // 3. Query Project Financials & Milestones
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

  // 4. Stage Daily Worklog Entry
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

  // 5. Stage Material Delivery Receipt
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

  // 6. Stage Contractor Payment
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

  // 7. Stage Project Expense Voucher
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

  // 8. Navigation Tool
  {
    name: 'navigate_app_page',
    description: 'Navigate to any page in Constructor (e.g. Contractor Accounts, Weekly Pay-Day, Daily Worklog, Material Reconciliation).',
    parameters: {
      type: 'object',
      properties: {
        targetPage: { type: 'string', description: 'Target route (e.g. /financials/contractors, /worklog, /materials/reconciliation, /financials/payday, /projects)' },
        pageName: { type: 'string', description: 'Display name of page' },
      },
      required: ['targetPage'],
    },
  },
];

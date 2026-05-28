

// ─── Core Data Types ───────────────────────────────────────────────────────

export type Project = {
  id: string;
  name: string;
  description: string;
  // DB uses snake_case; both aliases provided for compatibility
  start_date: string;
  end_date?: string;
  /** @deprecated Use start_date */
  startDate?: string;
  /** @deprecated Use end_date */
  endDate?: string;
  // DB CHECK: 'active' | 'completed' | 'on-hold'
  status: 'active' | 'completed' | 'on-hold';
  progress?: number;
  thumbnail_url?: string;
  company_id?: string;
  /** @deprecated Use company_id */
  companyId?: string;
  client_name?: string;
  /** @deprecated Use client_name */
  clientName?: string;
  client_contact?: string;
  /** @deprecated Use client_contact */
  clientContact?: string;
  location?: string;
  budget?: number;
  project_type?: string;
  /** @deprecated Use project_type */
  projectType?: string;
  team_size?: number;
  created_at?: string;
  updated_at?: string;
};

export type User = {
  id: string;
  email: string;
  displayName: string;
  phone?: string;
  photoURL?: string;
  role: 'admin' | 'manager' | 'member';
  projectIds: string[];
  permissions?: Record<string, any>;
  companyId: string | null;
  status: 'active' | 'inactive';
  created_at?: string;
};

export type Company = {
  id: string;
  name: string;
  ownerId: string;
  address: string;
  phone: string;
  website: string;
  businessType: 'general_contractor' | 'sub_contractor' | 'developer' | 'architectural_firm' | 'other';
  companySize: '1-10' | '11-50' | '51-200' | '201-500' | '500+';
  createdAt: string;
};

export type Invite = {
  id: string;
  email: string;
  companyId: string;
  role: 'admin' | 'manager' | 'member';
  projectIds?: string[];
  permissions?: Record<string, any>;
  status: 'pending' | 'accepted';
  createdAt: string;
};

export type ChatMessage = {
  id: string;
  projectId: string;
  userId: string;
  timestamp: string;
  message: string;
  fileAttachments?: string[];
};

export type Material = {
  id: string;
  name: string;
  description?: string;
  unit: string;
  costPerUnit: number;
  category: string;
  currentStock: number;
  minStock: number;
  supplier: string;
  project_id?: string;
  projectId?: string;
  projectName?: string;
  site_id?: string;
  siteId?: string;
  siteName?: string;
};

export type Expense = {
  id: string;
  // DB column: project_id
  project_id: string;
  /** @deprecated Use project_id */
  projectId?: string;
  // DB column: user_id (or created_by)
  user_id: string;
  /** @deprecated Use user_id */
  userId?: string;
  created_by?: string;
  amount: number;
  category: string;
  description: string;
  // DB column: expense_date
  expense_date: string;
  /** @deprecated Use expense_date */
  expenseDate?: string;
  payment_status?: 'pending' | 'paid' | 'overdue' | 'cancelled';
  /** @deprecated Use payment_status */
  paymentStatus?: 'pending' | 'paid' | 'overdue' | 'cancelled';
  notes?: string;
  receiver?: string;
  receipt_url?: string;
  created_at?: string;
  /** @deprecated Use created_at */
  createdAt?: string;
  user?: {
    name?: string;
    displayName?: string;
    photoURL?: string | null;
  };
};

export type Notification = {
  id: string;
  userId: string; // The user this notification is for
  projectId: string; // The project this notification belongs to
  title: string;
  description: string;
  timestamp: string;
  read: boolean;
  link: string; // URL to navigate to when clicked
  category: 'team' | 'inventory' | 'approvals';
  icon: 'MessageSquare' | 'Package' | 'FileText';
};

export type Document = {
  id: string;
  projectId: string;
  uploaderId: string;
  name: string;
  url: string;
  size: number;
  type: string;
  category?: string;
  createdAt: string;
  isRead?: boolean; // Frontend only property
  uploader?: {
    name: string;
    photoURL?: string | null;
  };
};


// ─── Mock / Seed Data ──────────────────────────────────────────────────────

export const projects: Project[] = [
  {
    id: 'proj-1',
    name: 'Downtown Office Reno',
    description: 'A full-scale renovation of a downtown office building, including modernizing the facade and interior spaces.',
    start_date: '2024-01-15T00:00:00.000Z',
    startDate: '2024-01-15T00:00:00.000Z',
    status: 'active',
    progress: 75,
    company_id: 'company-1',
    companyId: 'company-1',
  },
  {
    id: 'proj-2',
    name: 'Suburban Villa',
    description: 'Construction of a new luxury suburban villa with a pool and landscaped gardens.',
    start_date: '2024-03-01T00:00:00.000Z',
    startDate: '2024-03-01T00:00:00.000Z',
    status: 'active',
    progress: 40,
    company_id: 'company-1',
    companyId: 'company-1',
  },
  {
    id: 'proj-3',
    name: 'Coastal Bridge Repair',
    description: 'Structural repairs and reinforcement of a major coastal bridge to withstand environmental factors.',
    start_date: '2024-02-20T00:00:00.000Z',
    startDate: '2024-02-20T00:00:00.000Z',
    status: 'active',
    progress: 20,
    company_id: 'company-1',
    companyId: 'company-1',
  },
  {
    id: 'proj-4',
    name: 'City Park Fountain',
    description: 'Installation of a new, ornate water fountain as the centerpiece of the city park.',
    start_date: '2023-11-10T00:00:00.000Z',
    startDate: '2023-11-10T00:00:00.000Z',
    end_date: '2024-01-05T00:00:00.000Z',
    endDate: '2024-01-05T00:00:00.000Z',
    status: 'completed',
    progress: 100,
    company_id: 'company-1',
    companyId: 'company-1',
  },
  {
    id: 'proj-5',
    name: 'High-Rise Apartments',
    description: 'Building a modern high-rise apartment complex with state-of-the-art amenities.',
    start_date: '2023-09-01T00:00:00.000Z',
    startDate: '2023-09-01T00:00:00.000Z',
    status: 'active',
    progress: 85,
    company_id: 'company-1',
    companyId: 'company-1',
  },
  {
    id: 'proj-6',
    name: 'Retail Center Build-out',
    description: 'Interior construction and build-out for several new stores in a retail shopping center.',
    start_date: '2024-04-10T00:00:00.000Z',
    startDate: '2024-04-10T00:00:00.000Z',
    status: 'active',
    progress: 60,
    company_id: 'company-1',
    companyId: 'company-1',
  },
  {
    id: 'proj-7',
    name: 'Historic Theatre Restoration',
    description: 'Painstaking restoration of a historic theatre to its original grandeur, including seating and stage.',
    start_date: '2024-05-15T00:00:00.000Z',
    startDate: '2024-05-15T00:00:00.000Z',
    status: 'on-hold',
    progress: 30,
    company_id: 'company-1',
    companyId: 'company-1',
  },
  {
    id: 'proj-8',
    name: 'Modern Lakeside House',
    description: 'A custom-built modern house featuring large glass walls with a view of the lake.',
    start_date: '2023-10-01T00:00:00.000Z',
    startDate: '2023-10-01T00:00:00.000Z',
    end_date: '2024-05-20T00:00:00.000Z',
    endDate: '2024-05-20T00:00:00.000Z',
    status: 'completed',
    progress: 100,
    company_id: 'company-1',
    companyId: 'company-1',
  },
];

export type PurchaseOrderItem = {
  id: string;
  po_id: string;
  material_id?: string | null;
  material_name: string;
  quantity: number;
  unit_price: number;
  total_price: number;
};

export type PurchaseOrder = {
  id: string;
  project_id: string;
  po_number: string;
  supplier_name: string;
  supplier_contact?: string | null;
  total_amount?: number | null;
  status: 'pending' | 'approved' | 'completed' | 'rejected';
  delivery_date?: string | null;
  special_instructions?: string | null;
  created_by?: string | null;
  approved_by?: string | null;
  created_at?: string;
  updated_at?: string;
  projects?: {
    name: string;
  } | null;
  creator?: {
    display_name?: string | null;
  } | null;
  approver?: {
    display_name?: string | null;
  } | null;
  purchase_order_items?: PurchaseOrderItem[];
};


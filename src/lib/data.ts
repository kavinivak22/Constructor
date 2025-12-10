

import { Timestamp } from "firebase/firestore";

export type Project = {
  id: string;
  name: string;
  description: string;
  startDate: string | Timestamp;
  endDate?: string | Timestamp;
  status: 'planning' | 'in progress' | 'completed';
  progress?: number;
  imageId?: string;
  companyLogo?: string;
  companyId: string;
  clientName?: string;
  clientContact?: string;
  location?: string;
  budget?: number;
  projectType?: string;
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
  createdAt: Timestamp | any;
}

export type Invite = {
  id: string;
  email: string;
  companyId: string;
  role: 'admin' | 'manager' | 'member';
  projectIds?: string[];
  permissions?: Record<string, any>;
  status: 'pending' | 'accepted';
  createdAt: Timestamp;
}

export type ChatMessage = {
  id: string;
  projectId: string;
  userId: string;
  timestamp: Timestamp;
  message: string;
  fileAttachments?: string[];
}

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
};

export type Expense = {
  id: string;
  projectId: string;
  userId: string;
  amount: number;
  category: string;
  description: string;
  expenseDate: Timestamp;
  createdAt?: Timestamp;
  user: {
    name: string;
    photoURL?: string | null;
  }
}

export type Notification = {
  id: string;
  userId: string; // The user this notification is for
  projectId: string; // The project this notification belongs to
  title: string;
  description: string;
  timestamp: Timestamp;
  read: boolean;
  link: string; // URL to navigate to when clicked
  category: 'team' | 'inventory' | 'approvals';
  icon: 'MessageSquare' | 'Package' | 'FileText';
}

export type Document = {
  id: string;
  projectId: string;
  uploaderId: string;
  name: string;
  url: string;
  size: number;
  type: string;
  category?: string;
  createdAt: Timestamp;
  isRead?: boolean; // Frontend only property
  uploader?: {
    name: string;
    photoURL?: string | null;
  };
}


export const projects: Project[] = [
  {
    id: 'proj-1',
    name: 'Downtown Office Reno',
    description: 'A full-scale renovation of a downtown office building, including modernizing the facade and interior spaces.',
    startDate: '2024-01-15T00:00:00.000Z',
    status: 'in progress',
    progress: 75,
    imageId: 'downtown-office',
    companyId: 'company-1',
  },
  {
    id: 'proj-2',
    name: 'Suburban Villa',
    description: 'Construction of a new luxury suburban villa with a pool and landscaped gardens.',
    startDate: '2024-03-01T00:00:00.000Z',
    status: 'in progress',
    progress: 40,
    imageId: 'suburban-villa',
    companyId: 'company-1',
  },
  {
    id: 'proj-3',
    name: 'Coastal Bridge Repair',
    description: 'Structural repairs and reinforcement of a major coastal bridge to withstand environmental factors.',
    startDate: '2024-02-20T00:00:00.000Z',
    status: 'in progress',
    progress: 20,
    imageId: 'coastal-bridge',
    companyId: 'company-1',
  },
  {
    id: 'proj-4',
    name: 'City Park Fountain',
    description: 'Installation of a new, ornate water fountain as the centerpiece of the city park.',
    startDate: '2023-11-10T00:00:00.000Z',
    endDate: '2024-01-05T00:00:00.000Z',
    status: 'completed',
    progress: 100,
    imageId: 'city-park',
    companyId: 'company-1',
  },
  {
    id: 'proj-5',
    name: 'High-Rise Apartments',
    description: 'Building a modern high-rise apartment complex with state-of-the-art amenities.',
    startDate: '2023-09-01T00:00:00.000Z',
    status: 'in progress',
    progress: 85,
    imageId: 'high-rise',
    companyId: 'company-1',
  },
  {
    id: 'proj-6',
    name: 'Retail Center Build-out',
    description: 'Interior construction and build-out for several new stores in a retail shopping center.',
    startDate: '2024-04-10T00:00:00.000Z',
    status: 'in progress',
    progress: 60,
    imageId: 'retail-center',
    companyId: 'company-1',
  },
  {
    id: 'proj-7',
    name: 'Historic Theatre Restoration',
    description: 'Painstaking restoration of a historic theatre to its original grandeur, including seating and stage.',
    startDate: '2024-05-15T00:00:00.000Z',
    status: 'planning',
    progress: 30,
    imageId: 'historic-theatre',
    companyId: 'company-1',
  },
  {
    id: 'proj-8',
    name: 'Modern Lakeside House',
    description: 'A custom-built modern house featuring large glass walls with a view of the lake.',
    startDate: '2023-10-01T00:00:00.000Z',
    endDate: '2024-05-20T00:00:00.000Z',
    status: 'completed',
    progress: 100,
    imageId: 'lakeside-house',
    companyId: 'company-1',
  },
];

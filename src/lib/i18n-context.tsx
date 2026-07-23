'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';

export type Language = 'en' | 'ta';

type Dictionary = Record<string, Record<Language, string>>;

const dictionary: Dictionary = {
  // Navigation & Core UI
  dashboard: { en: 'Dashboard', ta: 'முகப்பு பலகை' },
  projects: { en: 'Projects', ta: 'திட்டங்கள்' },
  inventory: { en: 'Inventory', ta: 'சரக்கு இருப்பு' },
  purchaseOrders: { en: 'Purchase Orders', ta: 'கொள்முதல் ஆணைகள்' },
  dailyWorklog: { en: 'Daily Worklog', ta: 'தினசரி பணிப்பதிவு' },
  workPrep: { en: 'Work Prep', ta: 'பணி தயாரிப்பு' },
  teamHub: { en: 'Team Hub', ta: 'குழு மையம்' },
  analytics: { en: 'Analytics', ta: 'பகுப்பாய்வு' },
  aiEstimation: { en: 'AI Estimation', ta: 'AI மதிப்பீடு' },
  notifications: { en: 'Notifications', ta: 'அறிவிப்புகள்' },
  personalPouch: { en: 'Personal Pouch', ta: 'தனிப்பட்ட பை' },
  projectPouch: { en: 'Project Pouch', ta: 'திட்டப் பை' },
  financials: { en: 'Financials', ta: 'நிதி கணக்குகள்' },
  salaryProfiles: { en: 'Salary Profiles', ta: 'சம்பள சுயவிவரங்கள்' },
  weeklyPayday: { en: 'Weekly Pay-Day', ta: 'வாராந்திர சம்பள நாள்' },
  contractorAccounts: { en: 'Contractor Accounts', ta: 'ஒப்பந்ததாரர் கணக்குகள்' },
  projectExpenses: { en: 'Project Expenses', ta: 'திட்ட செலவுகள்' },
  materialReconciliation: { en: 'Material Reconciliation', ta: 'பொருள் கணக்கு சமரசம்' },
  clientMilestones: { en: 'Client Payment Milestones', ta: 'வாடிக்கையாளர் தவணை பணம்' },
  employees: { en: 'Employees', ta: 'ஊழியர்கள்' },

  // Section Headers
  aiTools: { en: 'AI Tools', ta: 'AI கருவிகள்' },
  pouchSection: { en: 'Pouch', ta: 'பணப்பை' },
  financialsSection: { en: 'Financials', ta: 'நிதி கணக்குகள்' },
  adminSection: { en: 'Admin', ta: 'நிர்வாகம்' },

  // Common Actions
  viewLedger: { en: 'View Ledger', ta: 'கணக்கை பார்க்க' },
  backToAccounts: { en: 'Back to Accounts', ta: 'கணக்கு பட்டியலுக்கு திரும்புக' },
  search: { en: 'Search...', ta: 'தேடுக...' },
  save: { en: 'Save', ta: 'சேமிக்க' },
  cancel: { en: 'Cancel', ta: 'ரத்து செய்' },
  approved: { en: 'Approved', ta: 'அங்கீகரிக்கப்பட்டது' },
  pending: { en: 'Pending', ta: 'நிலுவையில் உள்ளது' },
  paid: { en: 'Paid', ta: 'செலுத்தப்பட்டது' },

  // Worker Categories / Roles
  mason: { en: 'Mason', ta: 'கொத்தனார்' },
  maleHelper: { en: 'Male Labour / Helper', ta: 'ஆம்பள ஆளு' },
  femaleHelper: { en: 'Female Labour / Helper', ta: 'பொம்பள ஆளு' },
  barBender: { en: 'Bar Bender', ta: 'கம்பி கட்டும் ஆளு' },
  shutteringWorker: { en: 'Shuttering / Centering', ta: 'ஷட்டரிங் வேலை' },
  plumber: { en: 'Plumber', ta: 'பிளம்பர்' },
  electrician: { en: 'Electrician', ta: 'எலக்ட்ரீஷியன்' },
  painter: { en: 'Painter', ta: 'பெயிண்டர்' },

  // Material Terms
  cement: { en: 'Cement', ta: 'சிமெண்ட்' },
  tmtSteel: { en: 'TMT Steel', ta: 'TMT இரும்பு' },
  mSand: { en: 'M-Sand', ta: 'M-மணல்' },
  riverSand: { en: 'River Sand', ta: 'ஆற்று மணல்' },
  aacBlocks: { en: 'AAC Blocks', ta: 'AAC பிளாக்ஸ்' },
  redBricks: { en: 'Red Bricks', ta: 'செங்கல்' },
  blueMetal: { en: 'Blue Metal / Jelly', ta: 'ஜெல்லி' },
};

interface I18nContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: (key: string, fallback?: string) => string;
}

const I18nContext = createContext<I18nContextType>({
  language: 'en',
  setLanguage: () => {},
  t: (key: string, fallback?: string) => fallback || key,
});

export const I18nProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [language, setLanguageState] = useState<Language>('en');

  useEffect(() => {
    const saved = localStorage.getItem('constructor_lang') as Language;
    if (saved === 'en' || saved === 'ta') {
      setLanguageState(saved);
    }
  }, []);

  const setLanguage = (lang: Language) => {
    setLanguageState(lang);
    localStorage.setItem('constructor_lang', lang);
  };

  const t = (key: string, fallback?: string): string => {
    if (dictionary[key] && dictionary[key][language]) {
      return dictionary[key][language];
    }
    return fallback || key;
  };

  return (
    <I18nContext.Provider value={{ language, setLanguage, t }}>
      {children}
    </I18nContext.Provider>
  );
};

export const useI18n = () => useContext(I18nContext);

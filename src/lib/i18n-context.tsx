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

  // Common Actions & Phrase Map
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

// Full Phrase Translation Mapping for Universal App Text Replacement
const phraseMap: Record<string, string> = {
  'Contractor Accounts & Ledgers': 'ஒப்பந்ததாரர் கணக்குகள் & பேரேடு',
  'Track and reconcile contractor square-footage Rate contracts and NMR labor wage accounts separately for settlements.': 'சதுர அடி ஒப்பந்தம் மற்றும் NMR தினசரி கூலி கணக்குகளை தனித்தனியாக பராமரிக்கவும்.',
  'View by Contractor': 'ஒப்பந்ததாரர் வாரியாக பார்க்க',
  'View by Building / Site': 'கட்டிடம் / தளம் வாரியாக பார்க்க',
  'Rate Contract Outstanding': 'சதுர அடி ஒப்பந்த நிலுவை',
  'Rate Contract Settled': 'சதுர அடி ஒப்பந்தம் வழங்கப்பட்டது',
  'NMR Labor Outstanding': 'NMR கூலி நிலுவை',
  'NMR Labor Settled': 'NMR கூலி வழங்கப்பட்டது',
  'Active Contractor Accounts': 'செயலில் உள்ள ஒப்பந்ததாரர் கணக்குகள்',
  'Consolidated ledger view showing contract vs daily wage outstanding balances.': 'ஒப்பந்தம் மற்றும் தினசரி கூலி நிலுவை தொகையின் ஒருங்கிணைந்த பார்வை.',
  'Contractor': 'ஒப்பந்ததாரர்',
  'Category': 'பிரிவு',
  'Rate Contract Account (Sq.Ft)': 'சதுர அடி ஒப்பந்த கணக்கு',
  'NMR Account (Daily Wages)': 'NMR தினசரி கூலி கணக்கு',
  'Actions': 'செயல்கள்',
  'Cleared': 'தீர்க்கப்பட்டது',
  'Outstanding': 'நிலுவையில் உள்ள தொகை',
  'View Ledger': 'கணக்கை பார்க்க',
  'View Site Ledger': 'தள கணக்கை பார்க்க',
  'Back to Accounts': 'கணக்கு பட்டியலுக்கு திரும்புக',
  'Combined Ledger': 'ஒருங்கிணைந்த கணக்கு',
  'Rate Account (Sq.Ft)': 'சதுர அடி கணக்கு',
  'NMR Account (Labor)': 'NMR கூலி கணக்கு',
  'Total Paid': 'மொத்தம் செலுத்தப்பட்டது',
  'Total Outstanding': 'மொத்த நிலுவை தொகை',
  'Date': 'தேதி',
  'Classification': 'வகைப்பாடு',
  'Details': 'விவரங்கள்',
  'Amount': 'தொகை',
  'Status': 'நிலை',
  'Go to Pay-Run Details': 'சம்பள விவரங்களுக்கு செல்லவும்',
  'Weekly Pay-Day': 'வாராந்திர சம்பள நாள்',
  'Salary Profiles': 'சம்பள சுயவிவரங்கள்',
  'Project Expenses': 'திட்ட செலவுகள்',
  'Material Reconciliation': 'பொருள் கணக்கு சமரசம்',
  'Client Payment Milestones': 'வாடிக்கையாளர் தவணை பணம்',
  'Daily Worklog': 'தினசரி பணிப்பதிவு',
  'Work Prep': 'பணி தயாரிப்பு',
  'Purchase Orders': 'கொள்முதல் ஆணைகள்',
  'Inventory': 'சரக்கு இருப்பு',
  'Projects': 'திட்டங்கள்',
  'Dashboard': 'முகப்பு பலகை',
  'Select Building:': 'கட்டிடத்தை தேர்ந்தெடுக்கவும்:',
  'Choose a building': 'ஒரு கட்டிடத்தை தேர்ந்தெடுக்கவும்',
  'Building Rate Outstanding': 'கட்டிட ஒப்பந்த நிலுவை',
  'Building Rate Settled': 'கட்டிட ஒப்பந்தம் வழங்கப்பட்டது',
  'Building NMR Outstanding': 'கட்டிட NMR கூலி நிலுவை',
  'Building NMR Settled': 'கட்டிட NMR கூலி வழங்கப்பட்டது',
  'Active Contractors on Site': 'தளத்தில் உள்ள ஒப்பந்ததாரர்கள்',
  'Contact Phone': 'தொலைபேசி எண்',
  'Email Address': 'மின்னஞ்சல் முகவரி',
  'Bank Details': 'வங்கி விவரங்கள்',
  'Wages Breakdown': 'கூலி விவரங்கள்',
  'Worker Category': 'தொழிலாளி பிரிவு',
  'Man-Days': 'வேலை நாட்கள்',
  'Daily Rate': 'தினசரி கூலி',
  'Subtotal': 'உப மொத்தம்',
  'Total Earned': 'மொத்த வருமானம்',
  'Total Received': 'மொத்தம் பெறப்பட்டது',
  'Total NMR Wages': 'மொத்த NMR கூலி',
  'Total NMR Paid': 'மொத்தம் வழங்கப்பட்ட NMR கூலி',
  'No transactions registered for this selection.': 'தேர்ந்தெடுக்கப்பட்ட பிரிவில் பரிவர்த்தனைகள் எதுவும் இல்லை.',
  'Search contractor or category...': 'ஒப்பந்ததாரர் அல்லது பிரிவை தேடுக...',
  'All Buildings': 'எல்லா கட்டிடங்களும்',
  'Settlement status': 'செட்டில்மென்ட் நிலை',
  'Notes / Remarks': 'குறிப்புகள் / குறிப்புரை',
  'General Contractor': 'பொது ஒப்பந்ததாரர்',
  'Paid Amount': 'செலுத்தப்பட்ட தொகை',
  'Settled Amount': 'தீர்க்கப்பட்ட தொகை',
  'Reference': 'குறிப்பு',
  'Close Statement': 'கணக்கு அறிக்கையை மூடுக',
  'Select a Building': 'ஒரு கட்டிடத்தை தேர்ந்தெடுக்கவும்',
  'No Contractors Found on Site': 'தளத்தில் ஒப்பந்ததாரர்கள் யாரும் இல்லை',
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

  // DOM Auto-Translation Engine Effect when language === 'ta'
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const translateDOMNode = (node: Node) => {
      if (node.nodeType === Node.TEXT_NODE && node.nodeValue) {
        const trimmed = node.nodeValue.trim();
        if (phraseMap[trimmed] && language === 'ta') {
          node.nodeValue = node.nodeValue.replace(trimmed, phraseMap[trimmed]);
        }
      } else if (node.nodeType === Node.ELEMENT_NODE) {
        // Skip script and style tags
        const elem = node as HTMLElement;
        if (elem.tagName === 'SCRIPT' || elem.tagName === 'STYLE' || elem.tagName === 'INPUT' || elem.tagName === 'TEXTAREA') {
          return;
        }
        node.childNodes.forEach(translateDOMNode);
      }
    };

    const runTranslation = () => {
      if (language === 'ta') {
        document.body.childNodes.forEach(translateDOMNode);
      }
    };

    runTranslation();

    const observer = new MutationObserver((mutations) => {
      if (language === 'ta') {
        mutations.forEach(mutation => {
          mutation.addedNodes.forEach(translateDOMNode);
        });
      }
    });

    observer.observe(document.body, { childList: true, subtree: true });

    return () => {
      observer.disconnect();
    };
  }, [language]);

  const setLanguage = (lang: Language) => {
    setLanguageState(lang);
    localStorage.setItem('constructor_lang', lang);
    if (lang === 'en' && typeof window !== 'undefined') {
      window.location.reload();
    }
  };

  const t = (key: string, fallback?: string): string => {
    if (language === 'ta') {
      if (dictionary[key] && dictionary[key].ta) {
        return dictionary[key].ta;
      }
      if (fallback && phraseMap[fallback]) {
        return phraseMap[fallback];
      }
      if (phraseMap[key]) {
        return phraseMap[key];
      }
    }
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


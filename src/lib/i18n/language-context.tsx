'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';

// Define available languages
export type Language = 'en' | 'hi' | 'ta' | 'te' | 'kn' | 'ml' | 'bn' | 'mr';

export const LANGUAGES: { code: Language; name: string; localName: string }[] = [
    { code: 'en', name: 'English', localName: 'English' },
    { code: 'hi', name: 'Hindi', localName: 'हिंदी' },
    { code: 'ta', name: 'Tamil', localName: 'தமிழ்' },
    { code: 'te', name: 'Telugu', localName: 'తెలుగు' },
    { code: 'kn', name: 'Kannada', localName: 'ಕನ್ನಡ' },
    { code: 'ml', name: 'Malayalam', localName: 'മലയാളം' },
    { code: 'bn', name: 'Bengali', localName: 'বাংলা' },
    { code: 'mr', name: 'Marathi', localName: 'मराठी' },
];

type Translations = Record<string, any>;

interface LanguageContextType {
    language: Language;
    setLanguage: (lang: Language) => void;
    t: (key: string) => string;
    isLoading: boolean;
    dir: 'ltr' | 'rtl';
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export function LanguageProvider({ children }: { children: React.ReactNode }) {
    const [language, setLanguageState] = useState<Language>('en');
    const [translations, setTranslations] = useState<Translations>({});
    const [isLoading, setIsLoading] = useState(true);

    // Load language from local storage on mount
    useEffect(() => {
        const savedLang = localStorage.getItem('app-language') as Language;
        if (savedLang && LANGUAGES.some(l => l.code === savedLang)) {
            setLanguageState(savedLang);
        }
        // We'll load the default language (English) immediately if nothing is saved
        if (!savedLang) {
            loadTranslations('en');
        }
    }, []);

    // Load translations whenever language changes
    useEffect(() => {
        loadTranslations(language);
        localStorage.setItem('app-language', language);
        document.documentElement.lang = language;
        document.documentElement.dir = 'ltr'; // All supported Indian languages are LTR
    }, [language]);

    // Simple deep merge helper
    const deepMerge = (target: any, source: any) => {
        for (const key in source) {
            if (source[key] instanceof Object && key in target) {
                Object.assign(source[key], deepMerge(target[key], source[key]));
            }
        }
        Object.assign(target || {}, source);
        return target;
    };

    const loadTranslations = async (lang: Language) => {
        setIsLoading(true);
        try {
            // Always load English first as base
            const enModule = await import('./locales/en.json');
            let finalTranslations = JSON.parse(JSON.stringify(enModule.default)); // Deep copy

            if (lang !== 'en') {
                try {
                    let localeModule;
                    switch (lang) {
                        case 'hi': localeModule = await import('./locales/hi.json'); break;
                        case 'ta': localeModule = await import('./locales/ta.json'); break;
                        case 'te': localeModule = await import('./locales/te.json'); break;
                        case 'kn': localeModule = await import('./locales/kn.json'); break;
                        case 'ml': localeModule = await import('./locales/ml.json'); break;
                        case 'bn': localeModule = await import('./locales/bn.json'); break;
                        case 'mr': localeModule = await import('./locales/mr.json'); break;
                        default: localeModule = null;
                    }

                    if (localeModule) {
                        // Merge English with target language
                        // We want target language to override English, but keep English where target is missing
                        // My deepMerge below might be backwards or too complex, let's use a simpler approach for JSON
                        // actually a simple recursive merge
                        const merge = (base: any, override: any) => {
                            const result = { ...base };
                            for (const key in override) {
                                if (override[key] && typeof override[key] === 'object' && !Array.isArray(override[key])) {
                                    result[key] = merge(base[key] || {}, override[key]);
                                } else {
                                    result[key] = override[key];
                                }
                            }
                            return result;
                        }
                        finalTranslations = merge(finalTranslations, localeModule.default);
                    }
                } catch (e) {
                    console.warn(`Failed to load translations for ${lang}, using English fallback`, e);
                }
            }

            setTranslations(finalTranslations);
        } catch (error) {
            console.error(`Critial: Failed to load translations`, error);
        } finally {
            setIsLoading(false);
        }
    };

    const setLanguage = (lang: Language) => {
        setLanguageState(lang);
    };

    // Nested key retrieval (e.g., 'common.submit')
    const t = (key: string): string => {
        const keys = key.split('.');
        let value: any = translations;

        for (const k of keys) {
            if (value && typeof value === 'object' && k in value) {
                value = value[k];
            } else {
                return key; // Return key if translation not found
            }
        }

        return typeof value === 'string' ? value : key;
    };

    return (
        <LanguageContext.Provider value={{ language, setLanguage, t, isLoading, dir: 'ltr' }}>
            {children}
        </LanguageContext.Provider>
    );
}

export function useTranslation() {
    const context = useContext(LanguageContext);
    if (context === undefined) {
        throw new Error('useTranslation must be used within a LanguageProvider');
    }
    return context;
}

'use client';

import { useTranslation, LANGUAGES, Language } from '@/lib/i18n/language-context';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Globe } from 'lucide-react';

export default function SettingsPage() {
    const { t, language, setLanguage } = useTranslation();

    const handleLanguageChange = (value: string) => {
        setLanguage(value as Language);
    };

    return (
        <div className="container mx-auto py-10 space-y-8">
            <div>
                <h1 className="text-3xl font-bold tracking-tight">{t('settings_page.title')}</h1>
                <p className="text-muted-foreground">
                    {t('settings_page.description')}
                </p>
            </div>

            <div className="grid gap-6">
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Globe className="h-5 w-5" />
                            {t('settings_page.language_region')}
                        </CardTitle>
                        <CardDescription>
                            {t('common.select_language_desc')}
                        </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="grid gap-2">
                            <Label htmlFor="language">{t('common.language')}</Label>
                            <Select value={language} onValueChange={handleLanguageChange}>
                                <SelectTrigger id="language" className="w-[280px]">
                                    <SelectValue placeholder={t('common.select_language')} />
                                </SelectTrigger>
                                <SelectContent>
                                    {LANGUAGES.map((lang) => (
                                        <SelectItem key={lang.code} value={lang.code}>
                                            <span className="font-medium">{lang.localName}</span>
                                            <span className="ml-2 text-muted-foreground text-xs">({lang.name})</span>
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    </CardContent>
                </Card>

                {/* Placeholder for future settings sections */}
                {/* <Card>
          <CardHeader>
            <CardTitle>{t('settings_page.appearance')}</CardTitle>
             <CardDescription>Customize the look and feel of the application.</CardDescription>
          </CardHeader>
          <CardContent>
            ...
          </CardContent>
        </Card> */}
            </div>
        </div>
    );
}

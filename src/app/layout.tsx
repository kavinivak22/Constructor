
import './globals.css';
import { SupabaseProvider } from '@/supabase/provider';
import { ReactQueryProvider } from '@/lib/react-query';
import { cn } from '@/lib/utils';
import { MainLayout } from '@/app/main-layout';

export const metadata = {
  title: 'Constructor',
  description: 'Construction project management made simple.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap"
          rel="stylesheet"
        />
        <link rel="icon" href="https://api.iconify.design/lucide:building-2.svg?color=%23f97316" />
      </head>
      <body
        className={cn(
          'font-body antialiased',
          'bg-secondary'
        )}
        suppressHydrationWarning
      >
        <ReactQueryProvider>
          <SupabaseProvider>
            <MainLayout>{children}</MainLayout>
          </SupabaseProvider>
        </ReactQueryProvider>
      </body>
    </html>
  );
}

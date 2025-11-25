'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useSupabase } from '@/supabase/provider';
import { Loader2, CheckCircle, AlertCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function TestDbPage() {
  const { supabase, user } = useSupabase();
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<{ status: 'success' | 'error'; message: string } | null>(null);

  const handleTestWrite = async () => {
    if (!user) {
      setResult({ status: 'error', message: 'You must be logged in to run this test.' });
      return;
    }

    setIsLoading(true);
    setResult(null);

    const testData = {
      userId: user.id,
      email: user.email,
      message: 'This is a successful test write.',
    };

    const { error } = await supabase.from('test_writes').insert([testData]);

    if (error) {
      setResult({ status: 'error', message: `Supabase write failed: ${error.message}` });
    } else {
      setResult({ status: 'success', message: `Successfully wrote to Supabase table: test_writes` });
    }
    setIsLoading(false);
  };

  return (
    <div className="flex flex-col h-full">
      <header className="flex items-center gap-2 p-4 border-b md:px-6 shrink-0 bg-background sticky top-0 z-10">
        <h1 className="text-xl md:text-2xl font-bold tracking-tight font-headline">
          Supabase Database Write Test
        </h1>
      </header>
      <main className="flex-1 p-4 overflow-y-auto md:p-6">
        <div className="max-w-2xl mx-auto">
          <Card>
            <CardHeader>
              <CardTitle>Test Database Connection</CardTitle>
              <CardDescription>
                Click the button below to attempt a simple write operation to your Supabase database.
                This will verify that the connection and security rules are configured correctly for basic operations.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <Button onClick={handleTestWrite} disabled={isLoading || !user}>
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Running Test...
                  </>
                ) : (
                  'Run DB Write Test'
                )}
              </Button>
              {!user && <p className="text-sm text-destructive">Please log in to perform the test.</p>}
              {result && (
                <div
                  className={cn(
                    'p-4 rounded-md text-sm',
                    result.status === 'success' && 'bg-green-100 text-green-800',
                    result.status === 'error' && 'bg-red-100 text-red-800'
                  )}
                >
                  <div className="flex items-start gap-3">
                    {result.status === 'success' ? (
                      <CheckCircle className="h-5 w-5" />
                    ) : (
                      <AlertCircle className="h-5 w-5" />
                    )}
                    <div className="flex-1">
                      <p className="font-bold">
                        {result.status === 'success' ? 'Success!' : 'Error'}
                      </p>
                      <p className="break-words">{result.message}</p>
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}

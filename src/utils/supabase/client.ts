import { createBrowserClient } from '@supabase/ssr'

export function createClient() {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
  if (!url || !anonKey) {
    throw new Error(`Client environment variables are missing! URL present: ${!!url}, AnonKey present: ${!!anonKey}`);
  }
  return createBrowserClient(url, anonKey);
}

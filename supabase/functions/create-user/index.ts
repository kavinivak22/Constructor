
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient, SupabaseClient } from 'https://esm.sh/@supabase/supabase-js@2';
import { corsHeaders } from '../_shared/cors.ts';
import { load } from "https://deno.land/std@0.224.0/dotenv/mod.ts";

// Load environment variables from local .env file.
await load({ export: true });

// Main function to handle requests
serve(async (req: Request) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    const { email, password, displayName } = await req.json();

    if (!email || !password || !displayName) {
      return new Response(
        JSON.stringify({ error: 'Email, password, and displayName are required.' }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      );
    }
    
    // Initialize Supabase admin client
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    );

    // Create the user in the auth schema
    const { data: authData, error: authError } = await supabaseAdmin.auth.admin.createUser({
      email,
      password,
      email_confirm: false, // Set to false to disable email verification
      user_metadata: { full_name: displayName }
    });

    if (authError) {
      // Forward the specific auth error from Supabase
      return new Response(
        JSON.stringify({ error: `Authentication error: ${authError.message}` }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      );
    }

    if (!authData?.user) {
        throw new Error('User object was not returned from auth.admin.createUser.');
    }

    const userId = authData.user.id;

    // Insert the user profile into the public.users table
    const { error: profileError } = await supabaseAdmin
      .from('users')
      .insert({
        id: userId,
        email: email,
        full_name: displayName, // Correct column name
        role: 'member', // Default role
        status: 'active', // Default status
      });

    if (profileError) {
      // If profile insertion fails, we should delete the auth user we just created
      await supabaseAdmin.auth.admin.deleteUser(userId);
      return new Response(
        JSON.stringify({ error: `Profile creation error: ${profileError.message}` }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 500,
        }
      );
    }

    // Return the created user data on success
    return new Response(JSON.stringify({ user: authData.user }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    });

  } catch (e) {
    // Catch any other unexpected errors (e.g., JSON parsing, client init)
    return new Response(JSON.stringify({ error: e.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500,
    });
  }
});

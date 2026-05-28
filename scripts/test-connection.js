const fs = require('fs');
const path = require('path');
const { createClient } = require('@supabase/supabase-js');

async function testConnection() {
    console.log('Starting connection test...');

    // 1. Read .env file manually
    const envPath = path.resolve(__dirname, '../.env');
    if (!fs.existsSync(envPath)) {
        console.error('❌ Error: .env file not found at', envPath);
        process.exit(1);
    }

    console.log('✅ Found .env file at:', envPath);

    const envContent = fs.readFileSync(envPath, 'utf8');
    const envVars = {};
    envContent.split('\n').forEach(line => {
        const match = line.match(/^([^=]+)=(.*)$/);
        if (match) {
            const key = match[1].trim();
            let value = match[2].trim();
            // Remove quotes if present
            if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.slice(1, -1);
            }
            envVars[key] = value;
        }
    });

    const supabaseUrl = envVars['NEXT_PUBLIC_SUPABASE_URL'];
    const supabaseKey = envVars['SUPABASE_SERVICE_ROLE_KEY'] || envVars['NEXT_PUBLIC_SUPABASE_ANON_KEY'];

    if (!supabaseUrl) {
        console.error('❌ Error: NEXT_PUBLIC_SUPABASE_URL not found in .env');
    } else {
        console.log('✅ Found NEXT_PUBLIC_SUPABASE_URL:', supabaseUrl);
    }

    if (!supabaseKey) {
        console.error('❌ Error: SUPABASE_SERVICE_ROLE_KEY or NEXT_PUBLIC_SUPABASE_ANON_KEY not found in .env');
    } else {
        console.log('✅ Found Supabase Key (length):', supabaseKey ? supabaseKey.length : 0);
    }

    if (!supabaseUrl || !supabaseKey) {
        process.exit(1);
    }

    // 2. Initialize Supabase Client
    console.log('Initializing Supabase client...');
    const supabase = createClient(supabaseUrl, supabaseKey, {
        auth: {
            autoRefreshToken: false,
            persistSession: false
        }
    });

    // 3. Test Connection
    console.log('Attempting to fetch data from "projects" table...');
    const { data, error, count } = await supabase
        .from('projects')
        .select('*', { count: 'exact', head: true });

    if (error) {
        console.error('❌ Connection Failed!');
        console.error('Error Message:', error.message);
        console.error('Error Details:', error);
        console.error('Hint: Check if your IP is allowed in Supabase, or if RLS policies are blocking access (if using anon key).');
    } else {
        console.log('✅ Connection Successful!');
        console.log(`Successfully connected to Supabase. Found ${count} rows in "projects" table (head request).`);
    }
}

testConnection();

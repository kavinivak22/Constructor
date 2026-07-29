'use server';

import { createClient } from '@/utils/supabase/server';

export type QualityCheckItem = {
  id: string;
  title: string;
  is_completed: boolean;
};

export type ProcessTask = {
  id: string;
  title: string;
  description?: string | null;
  status: 'pending' | 'in-progress' | 'completed';
  checklists: QualityCheckItem[];
};

export type SubheadingProcess = {
  id: string;
  title: string;
  weight: number; // percentage, sum to 100
  tasks: ProcessTask[];
};

export type HabitProfile = {
  id: string;
  name: string;
  description?: string;
  building_type?: string;
  processes: SubheadingProcess[];
  created_at: string;
  updated_at: string;
};

// Helper function to call Gemini REST API via native fetch (Zero external SDK dependencies)
async function callGeminiRestApi(prompt: string, apiKey: string): Promise<string> {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [
        {
          parts: [{ text: prompt }]
        }
      ]
    })
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Gemini API HTTP Error ${response.status}: ${errorText}`);
  }

  const data = await response.json();
  const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) throw new Error('Empty response from Gemini API');
  return text;
}

// ----------------------------------------------------
// 1. HABIT PROFILES (Master Templates) MANAGEMENT
// ----------------------------------------------------

export async function getHabitProfiles(): Promise<{ success: boolean; data?: HabitProfile[]; error?: string }> {
  try {
    const supabase = await createClient();
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) throw new Error('Unauthorized');

    const { data: userProfile } = await supabase
      .from('users')
      .select('company_id')
      .eq('id', user.id)
      .single();

    const companyId = userProfile?.company_id || user.id;

    // Fetch from company metadata or settings table
    const { data, error } = await supabase
      .from('company_settings')
      .select('setting_value')
      .eq('company_id', companyId)
      .eq('setting_key', 'habit_profiles')
      .maybeSingle();

    if (error && error.code !== 'PGRST116') {
      console.warn('Fallback fetching habit profiles:', error);
    }

    if (data?.setting_value) {
      const profiles = typeof data.setting_value === 'string' ? JSON.parse(data.setting_value) : data.setting_value;
      return { success: true, data: profiles };
    }

    // Default built-in standard templates if none saved yet
    const defaultProfiles: HabitProfile[] = [
      {
        id: 'tmpl-framed-villa',
        name: 'Residential Villa (Framed Structure)',
        description: 'Standard RCC frame construction workflow with footings, columns, slabs, and MEP.',
        building_type: 'Residential Villa',
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
        processes: [
          {
            id: 'p1',
            title: '1. Foundation & Substructure',
            weight: 20,
            tasks: [
              {
                id: 't1_1',
                title: 'Site Clearance & Grid Marking',
                status: 'pending',
                checklists: [
                  { id: 'c1', title: 'Verify site boundary & grid line offset pins', is_completed: false },
                  { id: 'c2', title: 'Check excavation benchmark level', is_completed: false }
                ]
              },
              {
                id: 't1_2',
                title: 'Earth Excavation & Footing Pit Digging',
                status: 'pending',
                checklists: [
                  { id: 'c3', title: 'Inspect hard strata soil bearing level', is_completed: false },
                  { id: 'c4', title: 'Ensure pit shoring and anti-termite spray', is_completed: false }
                ]
              },
              {
                id: 't1_3',
                title: 'PCC Bed Concreting (1:4:8)',
                status: 'pending',
                checklists: [
                  { id: 'c5', title: 'Verify 4-inch PCC bed thickness and level', is_completed: false },
                  { id: 'c6', title: 'Ensure water curing for 3 days', is_completed: false }
                ]
              },
              {
                id: 't1_4',
                title: 'Footing Steel Rebar Mat & Column Starters',
                status: 'pending',
                checklists: [
                  { id: 'c7', title: 'Verify mesh bar spacing (150mm c/c)', is_completed: false },
                  { id: 'c8', title: 'Check 50mm cover block placement under mesh', is_completed: false },
                  { id: 'c9', title: 'Check vertical column starter plumb alignment', is_completed: false }
                ]
              },
              {
                id: 't1_5',
                title: 'Footing Concreting (M20) & Backfilling',
                status: 'pending',
                checklists: [
                  { id: 'c10', title: 'Vibrator compaction during pour', is_completed: false },
                  { id: 'c11', title: 'Soil backfilling in 150mm layers with compaction', is_completed: false }
                ]
              }
            ]
          },
          {
            id: 'p2',
            title: '2. Plinth & Superstructure Frame',
            weight: 25,
            tasks: [
              {
                id: 't2_1',
                title: 'Plinth Beam Shuttering & Rebar',
                status: 'pending',
                checklists: [
                  { id: 'c12', title: 'Verify top steel and stirrup spacing (100-150mm)', is_completed: false },
                  { id: 'c13', title: 'Check DPC damp proof course membrane', is_completed: false }
                ]
              },
              {
                id: 't2_2',
                title: 'RCC Column Casting to Lintel Height',
                status: 'pending',
                checklists: [
                  { id: 'c14', title: 'Check column shuttering plumb alignment using plumb-bob', is_completed: false },
                  { id: 'c15', title: 'Verify 40mm column cover block placement', is_completed: false }
                ]
              },
              {
                id: 't2_3',
                title: 'Ground Floor Roof Slab & Beam Formwork',
                status: 'pending',
                checklists: [
                  { id: 'c16', title: 'Check props verticality & sole plate stability', is_completed: false },
                  { id: 'c17', title: 'Inspect main & distribution bar bent-up chairs', is_completed: false },
                  { id: 'c18', title: 'Electrical conduit pipes & fan box placement', is_completed: false }
                ]
              }
            ]
          },
          {
            id: 'p3',
            title: '3. Brick Masonry & Partition Walls',
            weight: 20,
            tasks: [
              {
                id: 't3_1',
                title: 'External AAC/Brick Wall Construction',
                status: 'pending',
                checklists: [
                  { id: 'c19', title: 'Soak clay bricks in water prior to masonry', is_completed: false },
                  { id: 'c20', title: 'Verify wall straightness using line thread and spirit level', is_completed: false }
                ]
              },
              {
                id: 't3_2',
                title: 'Window & Door Lintel Bands',
                status: 'pending',
                checklists: [
                  { id: 'c21', title: 'Check minimum 150mm lintel bearing length on masonry', is_completed: false }
                ]
              }
            ]
          },
          {
            id: 'p4',
            title: '4. MEP, Plastering & Flooring',
            weight: 20,
            tasks: [
              {
                id: 't4_1',
                title: 'Plumbing Concealed Piping & Electrical Chipping',
                status: 'pending',
                checklists: [
                  { id: 'c22', title: 'Perform 10 bar hydraulic pressure test on CPVC lines', is_completed: false }
                ]
              },
              {
                id: 't4_2',
                title: 'Internal Cement Plastering & Putty',
                status: 'pending',
                checklists: [
                  { id: 'c23', title: 'Fix chicken wire mesh over brick-concrete joints', is_completed: false }
                ]
              }
            ]
          },
          {
            id: 'p5',
            title: '5. Finishing, Painting & Handover',
            weight: 15,
            tasks: [
              {
                id: 't5_1',
                title: 'Tiling & Bathroom Waterproofing',
                status: 'pending',
                checklists: [
                  { id: 'c24', title: 'Perform 48-hr ponding test on toilet floor waterproofing', is_completed: false }
                ]
              },
              {
                id: 't5_2',
                title: 'Final Painting & Fixture Handover',
                status: 'pending',
                checklists: [
                  { id: 'c25', title: 'Inspect final coat finish and electrical socket testing', is_completed: false }
                ]
              }
            ]
          }
        ]
      },
      {
        id: 'tmpl-load-bearing',
        name: 'Residential Villa (Unframed / Load Bearing)',
        description: 'Traditional masonry load-bearing construction without RCC columns.',
        building_type: 'Load Bearing Villa',
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
        processes: [
          {
            id: 'lb_p1',
            title: '1. Trench Excavation & Strip Footing',
            weight: 25,
            tasks: [
              {
                id: 'lb_t1',
                title: 'Continuous Strip Footing & Brick Footing Offset',
                status: 'pending',
                checklists: [
                  { id: 'lb_c1', title: 'Verify trench depth & PCC bed level', is_completed: false }
                ]
              }
            ]
          },
          {
            id: 'lb_p2',
            title: '2. Load Bearing Brick Masonry & Lintel',
            weight: 35,
            tasks: [
              {
                id: 'lb_t2',
                title: 'Superstructure Masonry to Slab Level',
                status: 'pending',
                checklists: [
                  { id: 'lb_c2', title: 'Verify 9-inch load bearing wall thickness', is_completed: false }
                ]
              }
            ]
          },
          {
            id: 'lb_p3',
            title: '3. Roof Slab Casting & Finishing',
            weight: 40,
            tasks: [
              {
                id: 'lb_t3',
                title: 'RCC Slab Formwork, Rebar & Concreting',
                status: 'pending',
                checklists: [
                  { id: 'lb_c3', title: 'Check slab thickness & curing', is_completed: false }
                ]
              }
            ]
          }
        ]
      }
    ];

    return { success: true, data: defaultProfiles };
  } catch (error: any) {
    console.error('Error fetching habit profiles:', error);
    return { success: false, error: error.message };
  }
}

export async function saveHabitProfile(profile: Omit<HabitProfile, 'id' | 'created_at' | 'updated_at'> & { id?: string }): Promise<{ success: boolean; data?: HabitProfile; error?: string }> {
  try {
    const supabase = await createClient();
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) throw new Error('Unauthorized');

    const { data: userProfile } = await supabase
      .from('users')
      .select('company_id')
      .eq('id', user.id)
      .single();

    const companyId = userProfile?.company_id || user.id;

    // Fetch existing profiles
    const { data: existingData } = await supabase
      .from('company_settings')
      .select('setting_value')
      .eq('company_id', companyId)
      .eq('setting_key', 'habit_profiles')
      .maybeSingle();

    let existingProfiles: HabitProfile[] = [];
    if (existingData?.setting_value) {
      existingProfiles = typeof existingData.setting_value === 'string' ? JSON.parse(existingData.setting_value) : existingData.setting_value;
    } else {
      const defaultsRes = await getHabitProfiles();
      existingProfiles = defaultsRes.data || [];
    }

    const now = new Date().toISOString();
    let savedProfile: HabitProfile;

    if (profile.id) {
      existingProfiles = existingProfiles.map(p => {
        if (p.id === profile.id) {
          savedProfile = { ...p, ...profile, updated_at: now };
          return savedProfile;
        }
        return p;
      });
      // @ts-ignore
      if (!savedProfile!) {
        savedProfile = { ...profile, id: profile.id, created_at: now, updated_at: now };
        existingProfiles.push(savedProfile);
      }
    } else {
      savedProfile = {
        ...profile,
        id: `profile-${Date.now()}`,
        created_at: now,
        updated_at: now,
      };
      existingProfiles.push(savedProfile);
    }

    // Save to company_settings
    const { error: upsertError } = await supabase
      .from('company_settings')
      .upsert({
        company_id: companyId,
        setting_key: 'habit_profiles',
        setting_value: existingProfiles,
        updated_at: now
      }, { onConflict: 'company_id,setting_key' });

    if (upsertError) throw upsertError;

    return { success: true, data: savedProfile };
  } catch (error: any) {
    console.error('Error saving habit profile:', error);
    return { success: false, error: error.message };
  }
}

export async function deleteHabitProfile(profileId: string): Promise<{ success: boolean; error?: string }> {
  try {
    const supabase = await createClient();
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) throw new Error('Unauthorized');

    const { data: userProfile } = await supabase
      .from('users')
      .select('company_id')
      .eq('id', user.id)
      .single();

    const companyId = userProfile?.company_id || user.id;

    const { data: existingData } = await supabase
      .from('company_settings')
      .select('setting_value')
      .eq('company_id', companyId)
      .eq('setting_key', 'habit_profiles')
      .maybeSingle();

    if (!existingData?.setting_value) return { success: true };

    let existingProfiles: HabitProfile[] = typeof existingData.setting_value === 'string' ? JSON.parse(existingData.setting_value) : existingData.setting_value;
    existingProfiles = existingProfiles.filter(p => p.id !== profileId);

    await supabase
      .from('company_settings')
      .upsert({
        company_id: companyId,
        setting_key: 'habit_profiles',
        setting_value: existingProfiles,
        updated_at: new Date().toISOString()
      }, { onConflict: 'company_id,setting_key' });

    return { success: true };
  } catch (error: any) {
    return { success: false, error: error.message };
  }
}

// ----------------------------------------------------
// 2. PROJECT SCOPE & CHECKLIST MANAGEMENT
// ----------------------------------------------------

export async function getProjectScope(projectId: string): Promise<{
  success: boolean;
  processes?: SubheadingProcess[];
  appliedProfileId?: string;
  error?: string;
}> {
  try {
    const supabase = await createClient();

    // Primary: Read from projects.scope_data column
    try {
      const { data, error } = await supabase
        .from('projects')
        .select('scope_data')
        .eq('id', projectId)
        .single();

      if (!error && data?.scope_data) {
        const scope = typeof data.scope_data === 'string' ? JSON.parse(data.scope_data) : data.scope_data;
        return {
          success: true,
          processes: scope.processes || [],
          appliedProfileId: scope.appliedProfileId,
        };
      }
    } catch (colErr) {
      console.warn('Fallback querying project scope from company_settings:', colErr);
    }

    // Fallback: Read from company_settings table
    const { data: fallbackData } = await supabase
      .from('company_settings')
      .select('setting_value')
      .eq('setting_key', `scope_${projectId}`)
      .maybeSingle();

    if (fallbackData?.setting_value) {
      const scope = typeof fallbackData.setting_value === 'string' ? JSON.parse(fallbackData.setting_value) : fallbackData.setting_value;
      return {
        success: true,
        processes: scope.processes || [],
        appliedProfileId: scope.appliedProfileId,
      };
    }

    return { success: true, processes: [] };
  } catch (error: any) {
    console.error('Error fetching project scope:', error);
    return { success: false, error: error.message };
  }
}

export async function saveProjectScope(
  projectId: string,
  processes: SubheadingProcess[],
  appliedProfileId?: string,
  syncToMasterProfileId?: string
): Promise<{ success: boolean; progress?: number; error?: string }> {
  try {
    const supabase = await createClient();

    // 1. Calculate new overall project progress
    const totalProgress = calculateOverallProgressFromProcesses(processes);

    const scopePayload = {
      processes,
      appliedProfileId: appliedProfileId || null,
      updated_at: new Date().toISOString()
    };

    // Try updating project scope and calculated progress on projects table
    let primarySuccess = false;
    try {
      const { error: updateError } = await supabase
        .from('projects')
        .update({
          scope_data: scopePayload,
          progress: totalProgress,
          updated_at: new Date().toISOString()
        })
        .eq('id', projectId);

      if (!updateError) {
        primarySuccess = true;
      }
    } catch (e) {
      console.warn('Primary update to projects table scope_data failed, using company_settings fallback');
    }

    // Backup / Fallback: Save to company_settings
    const { data: { user } } = await supabase.auth.getUser();
    if (user) {
      const { data: userProfile } = await supabase
        .from('users')
        .select('company_id')
        .eq('id', user.id)
        .single();
      const companyId = userProfile?.company_id || user.id;

      await supabase
        .from('company_settings')
        .upsert({
          company_id: companyId,
          setting_key: `scope_${projectId}`,
          setting_value: scopePayload,
          updated_at: new Date().toISOString()
        }, { onConflict: 'company_id,setting_key' });

      // Always update overall project progress column
      await supabase
        .from('projects')
        .update({
          progress: totalProgress,
          updated_at: new Date().toISOString()
        })
        .eq('id', projectId);
    }

    // 2. If user checked "Make changes in [Profile Name] Profile too", sync master template
    if (syncToMasterProfileId) {
      const profilesRes = await getHabitProfiles();
      const existingProfile = profilesRes.data?.find(p => p.id === syncToMasterProfileId);
      if (existingProfile) {
        await saveHabitProfile({
          ...existingProfile,
          processes: processes.map(p => ({
            ...p,
            tasks: p.tasks.map(t => ({
              ...t,
              status: 'pending',
              checklists: t.checklists.map(c => ({ ...c, is_completed: false }))
            }))
          }))
        });
      }
    }

    return { success: true, progress: totalProgress };
  } catch (error: any) {
    console.error('Error saving project scope:', error);
    return { success: false, error: error.message };
  }
}

export async function applyProfileToProject(projectId: string, profileId: string): Promise<{ success: boolean; progress?: number; error?: string }> {
  try {
    const profilesRes = await getHabitProfiles();
    const targetProfile = profilesRes.data?.find(p => p.id === profileId);

    if (!targetProfile) throw new Error('Selected habit profile not found.');

    // Clone processes with fresh reset statuses
    const clonedProcesses: SubheadingProcess[] = targetProfile.processes.map((proc, pIdx) => ({
      ...proc,
      id: `proc_${pIdx}_${Date.now()}`,
      tasks: proc.tasks.map((task, tIdx) => ({
        ...task,
        id: `task_${pIdx}_${tIdx}_${Date.now()}`,
        status: 'pending',
        checklists: task.checklists.map((chk, cIdx) => ({
          ...chk,
          id: `chk_${pIdx}_${tIdx}_${cIdx}_${Date.now()}`,
          is_completed: false
        }))
      }))
    }));

    return await saveProjectScope(projectId, clonedProcesses, profileId);
  } catch (error: any) {
    console.error('Error applying profile to project:', error);
    return { success: false, error: error.message };
  }
}

// Helper formula to compute overall progress
function calculateOverallProgressFromProcesses(processes: SubheadingProcess[]): number {
  if (!processes || processes.length === 0) return 0;

  let totalWeightedScore = 0;
  let totalWeight = 0;

  for (const proc of processes) {
    const weight = proc.weight || 0;
    totalWeight += weight;

    if (!proc.tasks || proc.tasks.length === 0) continue;

    let taskScoreSum = 0;
    for (const task of proc.tasks) {
      if (task.status === 'completed') {
        taskScoreSum += 1.0;
      } else if (task.status === 'in-progress') {
        // If task has checklist items, calculate partial ratio
        if (task.checklists && task.checklists.length > 0) {
          const completedChecks = task.checklists.filter(c => c.is_completed).length;
          taskScoreSum += (completedChecks / task.checklists.length);
        } else {
          taskScoreSum += 0.5;
        }
      } else if (task.checklists && task.checklists.length > 0) {
        const completedChecks = task.checklists.filter(c => c.is_completed).length;
        if (completedChecks > 0) {
          taskScoreSum += (completedChecks / task.checklists.length);
        }
      }
    }

    const processCompletionRatio = taskScoreSum / proc.tasks.length;
    totalWeightedScore += (processCompletionRatio * weight);
  }

  if (totalWeight === 0) return 0;
  // Normalize if weights don't sum to exactly 100
  const normalizedProgress = (totalWeightedScore / totalWeight) * 100;
  return Math.min(100, Math.round(normalizedProgress));
}

// ----------------------------------------------------
// 3. TOKEN-OPTIMIZED EVENT-DRIVEN GEMINI AI ENGINE
// ----------------------------------------------------

export async function evaluateWorklogProgressWithGemini(projectId: string, worklogId: string): Promise<{ success: boolean; newProgress?: number; aiInsight?: string; error?: string }> {
  try {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      console.warn('GEMINI_API_KEY not configured, skipping AI progress matching.');
      return { success: true };
    }

    const supabase = await createClient();

    // 1. Fetch worklog details
    const { data: worklog, error: logError } = await supabase
      .from('daily_worklogs')
      .select('*, labor:worklog_labor_entries(*), materials:worklog_material_entries(*), photos:worklog_photos(*)')
      .eq('id', worklogId)
      .single();

    if (logError || !worklog) throw new Error('Worklog not found for evaluation');

    // 2. Fetch current project scope
    const scopeRes = await getProjectScope(projectId);
    const processes = scopeRes.processes || [];
    if (processes.length === 0) return { success: true };

    // Format concise payload for Gemini AI to minimize token usage
    const logSummary = {
      title: worklog.title,
      date: worklog.date,
      laborWork: worklog.labor?.map((l: any) => `${l.contractor_name} (${l.category}): ${l.work_description || ''}`).join('; '),
      materials: worklog.materials?.map((m: any) => `${m.material_name} (${m.quantity_consumed} ${m.unit || ''})`).join('; '),
      photoCaptions: worklog.photos?.map((p: any) => p.caption).filter(Boolean).join('; ')
    };

    const pendingTasksList = processes.flatMap(p => 
      p.tasks.filter(t => t.status !== 'completed').map(t => ({
        processId: p.id,
        taskId: t.id,
        processTitle: p.title,
        taskTitle: t.title,
        checklists: t.checklists.map(c => ({ id: c.id, title: c.title, done: c.is_completed }))
      }))
    );

    if (pendingTasksList.length === 0) return { success: true };

    const prompt = `
You are an expert construction site supervisor AI. Analyze today's daily worklog and match executed work to the project's pending tasks and quality checklist items.

Daily Worklog:
${JSON.stringify(logSummary, null, 2)}

Pending Project Tasks & Quality Checklists:
${JSON.stringify(pendingTasksList, null, 2)}

Output ONLY a raw JSON object (no markdown, no backticks) with:
{
  "completedTaskIds": ["task_id_1"],
  "inProgressTaskIds": ["task_id_2"],
  "completedChecklistIds": ["chk_id_1", "chk_id_2"],
  "aiInsight": "Short 1-sentence progress summary of work accomplished today."
}
`;

    const responseText = await callGeminiRestApi(prompt, apiKey);
    const cleanedText = responseText.trim().replace(/^```json/, '').replace(/```$/, '').trim();
    const aiOutput = JSON.parse(cleanedText);

    // Apply AI updates to processes array
    let changesMade = false;
    const updatedProcesses = processes.map(proc => ({
      ...proc,
      tasks: proc.tasks.map(task => {
        let newStatus = task.status;
        if (aiOutput.completedTaskIds?.includes(task.id)) {
          newStatus = 'completed';
          changesMade = true;
        } else if (aiOutput.inProgressTaskIds?.includes(task.id) && task.status === 'pending') {
          newStatus = 'in-progress';
          changesMade = true;
        }

        const updatedChecklists = task.checklists.map(chk => {
          if (aiOutput.completedChecklistIds?.includes(chk.id) && !chk.is_completed) {
            changesMade = true;
            return { ...chk, is_completed: true };
          }
          return chk;
        });

        // Auto mark task complete if all checklists done
        if (updatedChecklists.length > 0 && updatedChecklists.every(c => c.is_completed)) {
          newStatus = 'completed';
        }

        return {
          ...task,
          status: newStatus,
          checklists: updatedChecklists
        };
      })
    }));

    if (changesMade) {
      const saveRes = await saveProjectScope(projectId, updatedProcesses, scopeRes.appliedProfileId);
      return {
        success: true,
        newProgress: saveRes.progress,
        aiInsight: aiOutput.aiInsight
      };
    }

    return { success: true, aiInsight: aiOutput.aiInsight };
  } catch (error: any) {
    console.error('Error evaluating worklog with Gemini:', error);
    return { success: false, error: error.message };
  }
}

export async function generateTaskChecklistWithGemini(taskTitle: string, buildingType: string = 'Residential Villa'): Promise<{ success: boolean; checklists?: string[]; error?: string }> {
  try {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      return {
        success: true,
        checklists: [
          'Verify dimensions and layout alignment',
          'Inspect material quality and grade specs',
          'Ensure safety gear and site protection',
          'Clean area upon work completion'
        ]
      };
    }

    const prompt = `
Generates 4 to 6 concise, highly practical engineering quality control checklist items for the construction task: "${taskTitle}" on a ${buildingType} project.

Return ONLY a raw JSON array of strings:
["Checklist item 1", "Checklist item 2", "Checklist item 3"]
`;

    const responseText = await callGeminiRestApi(prompt, apiKey);
    const cleanedText = responseText.trim().replace(/^```json/, '').replace(/```$/, '').trim();
    const checklists: string[] = JSON.parse(cleanedText);

    return { success: true, checklists };
  } catch (error: any) {
    console.error('Error generating AI task checklist:', error);
    return {
      success: true,
      checklists: [
        'Verify dimensions and level alignment',
        'Inspect material quality & mix ratio',
        'Check compaction/curing requirement'
      ]
    };
  }
}

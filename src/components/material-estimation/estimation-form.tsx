"use client";

import { useActionState, useEffect, useRef, useState, useTransition } from "react";
import { useFormStatus } from "react-dom";
import {
  getMaterialEstimation,
  getAssignedProjects,
  parseEstimationReport,
  importProjectMaterials,
  type FormState
} from "@/app/material-estimation/actions";
import { type ParsedMaterial } from "@/ai/flows/parse-estimation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { Loader2, Wand2, ArrowRight, Table, Save, Plus, Trash2, CheckCircle2 } from "lucide-react";

interface ProjectOption {
  id: string;
  name: string;
}

const initialState: FormState = {
  message: "",
};

function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <Button type="submit" disabled={pending} size="lg" className="w-full sm:w-auto">
      {pending ? (
        <>
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          Analyzing & Generating...
        </>
      ) : (
        <>
          <Wand2 className="mr-2 h-4 w-4" />
          Generate Report
        </>
      )}
    </Button>
  );
}

export function EstimationForm() {
  const [state, formAction] = useActionState(getMaterialEstimation, initialState);
  const { toast } = useToast();
  const formRef = useRef<HTMLFormElement>(null);
  const [isPending, startTransition] = useTransition();

  // Project select state
  const [projects, setProjects] = useState<ProjectOption[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");
  const [loadingProjects, setLoadingProjects] = useState(true);

  // Parsing & structured materials states
  const [parsing, setParsing] = useState(false);
  const [parsedMaterials, setParsedMaterials] = useState<ParsedMaterial[]>([]);
  const [importing, setImporting] = useState(false);
  const [imported, setImported] = useState(false);

  // Fetch projects on mount
  useEffect(() => {
    async function loadProjects() {
      try {
        const data = await getAssignedProjects();
        setProjects(data);
        if (data.length > 0) {
          setSelectedProjectId(data[0].id);
        }
      } catch (err) {
        console.error("Failed to load projects", err);
      } finally {
        setLoadingProjects(false);
      }
    }
    loadProjects();
  }, []);

  // Handle estimation successful generated
  useEffect(() => {
    if (!state.message || state.message === initialState.message) return;

    if (state.message !== "Estimation successful.") {
      toast({
        title: "Error",
        description: state.message,
        variant: "destructive",
      });
    } else {
      toast({
        title: "Report Generated",
        description: "Your text-based estimation report is ready.",
      });
      // Reset parsing and import states for new report
      setParsedMaterials([]);
      setImported(false);
    }
  }, [state, toast]);

  // Parse raw text to structured format
  const handleParseEstimation = async () => {
    if (!state.estimation) return;
    setParsing(true);
    try {
      const materials = await parseEstimationReport(state.estimation);
      setParsedMaterials(materials);
      toast({
        title: "Analysis Complete",
        description: `Successfully extracted ${materials.length} material items.`,
      });
    } catch (err: any) {
      toast({
        title: "Extraction Failed",
        description: err.message || "Failed to parse estimation text.",
        variant: "destructive",
      });
    } finally {
      setParsing(false);
    }
  };

  // Grid/Editable fields Handlers
  const handleMaterialChange = (index: number, field: keyof ParsedMaterial, value: any) => {
    setParsedMaterials(prev => prev.map((item, idx) => {
      if (idx !== index) return item;
      return {
        ...item,
        [field]: field === "quantity" || field === "cost" ? Number(value) : value
      };
    }));
  };

  const handleAddMaterial = () => {
    const newItem: ParsedMaterial = {
      name: "New Material Item",
      category: "General",
      quantity: 1,
      unit: "pcs",
      cost: 0
    };
    setParsedMaterials(prev => [...prev, newItem]);
  };

  const handleDeleteMaterial = (index: number) => {
    setParsedMaterials(prev => prev.filter((_, idx) => idx !== index));
  };

  // Import structured materials to project materials db table
  const handleImportMaterials = async () => {
    if (!selectedProjectId) {
      toast({
        title: "Project Required",
        description: "Please select a target project from the dropdown first.",
        variant: "destructive"
      });
      return;
    }

    if (parsedMaterials.length === 0) {
      toast({
        title: "No Materials",
        description: "Please parse the report or add material items to import.",
        variant: "destructive"
      });
      return;
    }

    setImporting(true);
    try {
      await importProjectMaterials(selectedProjectId, parsedMaterials);
      setImported(true);
      toast({
        title: "Import Successful",
        description: "All material specifications have been synchronized with your project inventory.",
      });
    } catch (err: any) {
      toast({
        title: "Import Failed",
        description: err.message || "Failed to insert materials into project database.",
        variant: "destructive"
      });
    } finally {
      setImporting(false);
    }
  };

  return (
    <div className="space-y-8">
      <form ref={formRef} action={formAction} className="space-y-6">
        <Card className="glass-card">
          <CardHeader>
            <CardTitle className="font-headline">Project Specifications</CardTitle>
            <CardDescription>
              Configure the dimensions and parameters for AI material estimations.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Target Project Dropdown */}
            <div className="space-y-2">
              <Label htmlFor="target-project">Target Project for Inventory Import</Label>
              {loadingProjects ? (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Loading assigned projects...
                </div>
              ) : projects.length === 0 ? (
                <div className="text-sm text-destructive font-semibold">
                  No active projects found. Please create a project before importing materials.
                </div>
              ) : (
                <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
                  <SelectTrigger id="target-project" className="bg-background">
                    <SelectValue placeholder="Select a project..." />
                  </SelectTrigger>
                  <SelectContent>
                    {projects.map(p => (
                      <SelectItem key={p.id} value={p.id}>{p.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>

            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="projectType">Project Type</Label>
                <Input id="projectType" name="projectType" placeholder="e.g., Residential, Commercial" required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="projectSize">Project Size (sq ft)</Label>
                <Input id="projectSize" name="projectSize" placeholder="e.g., 2500" required />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="projectLocation">Project Location</Label>
              <Input id="projectLocation" name="projectLocation" placeholder="e.g., Austin, TX" required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="specificRequirements">Specific Requirements</Label>
              <Textarea
                id="specificRequirements"
                name="specificRequirements"
                placeholder="e.g., Eco-friendly materials, specific brand of windows"
                required
                className="min-h-[120px]"
              />
            </div>
            {state.issues && state.issues.length > 0 && (
              <ul className="text-sm text-destructive list-disc list-inside space-y-1">
                {state.issues.map((issue, i) => <li key={i}>{issue}</li>)}
              </ul>
            )}
          </CardContent>
          <CardFooter>
            <SubmitButton />
          </CardFooter>
        </Card>
      </form>

      {state.estimation && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Plain Text Report Card */}
          <Card className="glass-card h-fit">
            <CardHeader className="flex flex-row justify-between items-center pb-2">
              <div>
                <CardTitle className="font-headline flex items-center gap-2">
                  <Wand2 className="text-primary h-5 w-5" />
                  1. Estimation Report
                </CardTitle>
                <CardDescription>Generated text breakdown from AI assistant</CardDescription>
              </div>
              <Button
                onClick={handleParseEstimation}
                disabled={parsing}
                className="bg-primary hover:bg-primary/95 text-xs h-9 px-3 shrink-0"
              >
                {parsing ? (
                  <>
                    <Loader2 className="h-3 w-3 animate-spin mr-1.5" />
                    Analyzing...
                  </>
                ) : (
                  <>
                    Parse to Inventory
                    <ArrowRight className="h-3 w-3 ml-1.5" />
                  </>
                )}
              </Button>
            </CardHeader>
            <CardContent>
              <div className="prose prose-sm max-w-none dark:prose-invert whitespace-pre-wrap font-body bg-white/10 dark:bg-black/20 border border-white/10 dark:border-black/10 p-4 rounded-lg max-h-[500px] overflow-y-auto">
                {state.estimation}
              </div>
            </CardContent>
          </Card>

          {/* Structured Inventory Grid Card */}
          <Card className="glass-card">
            <CardHeader className="flex flex-row justify-between items-center pb-2">
              <div>
                <CardTitle className="font-headline flex items-center gap-2">
                  <Table className="text-indigo-500 h-5 w-5" />
                  2. Import Preview Inventory
                </CardTitle>
                <CardDescription>Review and modify structured material listings</CardDescription>
              </div>
              {parsedMaterials.length > 0 && !imported && (
                <Button
                  onClick={handleImportMaterials}
                  disabled={importing}
                  className="bg-emerald-600 hover:bg-emerald-700 text-xs h-9 px-3 shrink-0"
                >
                  {importing ? (
                    <>
                      <Loader2 className="h-3 w-3 animate-spin mr-1.5" />
                      Importing...
                    </>
                  ) : (
                    <>
                      <Save className="h-3 w-3 mr-1.5" />
                      Save to Project
                    </>
                  )}
                </Button>
              )}
            </CardHeader>
            <CardContent>
              {parsedMaterials.length === 0 ? (
                <div className="flex flex-col items-center justify-center p-12 text-center border-2 border-dashed rounded-lg bg-muted/20 min-h-[300px]">
                  <Table className="h-12 w-12 text-muted-foreground mb-3 opacity-60" />
                  <p className="font-medium text-sm">No structured data extracted yet</p>
                  <p className="text-xs text-muted-foreground max-w-xs mt-1">
                    Click "Parse to Inventory" in the estimation report card to generate a structured dataset.
                  </p>
                </div>
              ) : imported ? (
                <div className="flex flex-col items-center justify-center p-12 text-center border border-emerald-500/20 rounded-lg bg-emerald-500/5 min-h-[300px]">
                  <CheckCircle2 className="h-16 w-16 text-emerald-500 mb-4 animate-bounce" />
                  <h3 className="text-lg font-bold text-emerald-500">Materials Imported Successfully!</h3>
                  <p className="text-sm text-muted-foreground max-w-sm mt-2">
                    All items have been added to the target project's materials database. You can now view and track them in the main Materials dashboard.
                  </p>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="max-h-[400px] overflow-y-auto pr-1 border border-white/10 dark:border-black/10 rounded-lg divide-y divide-white/10 dark:divide-black/10 bg-white/5 dark:bg-black/10">
                    {parsedMaterials.map((item, idx) => (
                      <div key={idx} className="p-3 space-y-2 hover:bg-muted/40 transition-colors">
                        <div className="flex gap-2">
                          <div className="flex-1">
                            <Input
                              value={item.name}
                              onChange={e => handleMaterialChange(idx, "name", e.target.value)}
                              placeholder="Material Name"
                              className="text-sm font-semibold h-8 bg-background"
                            />
                          </div>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 hover:bg-destructive/10 text-destructive shrink-0"
                            onClick={() => handleDeleteMaterial(idx)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                          <div>
                            <span className="text-[10px] text-muted-foreground uppercase font-bold block mb-0.5">Category</span>
                            <Input
                              value={item.category}
                              onChange={e => handleMaterialChange(idx, "category", e.target.value)}
                              placeholder="Concrete"
                              className="text-xs h-7 bg-background"
                            />
                          </div>
                          <div>
                            <span className="text-[10px] text-muted-foreground uppercase font-bold block mb-0.5">Quantity</span>
                            <Input
                              type="number"
                              value={item.quantity}
                              onChange={e => handleMaterialChange(idx, "quantity", e.target.value)}
                              placeholder="10"
                              className="text-xs h-7 bg-background"
                            />
                          </div>
                          <div>
                            <span className="text-[10px] text-muted-foreground uppercase font-bold block mb-0.5">Unit</span>
                            <Input
                              value={item.unit}
                              onChange={e => handleMaterialChange(idx, "unit", e.target.value)}
                              placeholder="bags"
                              className="text-xs h-7 bg-background"
                            />
                          </div>
                          <div>
                            <span className="text-[10px] text-muted-foreground uppercase font-bold block mb-0.5">Unit Cost (₹)</span>
                            <Input
                              type="number"
                              value={item.cost}
                              onChange={e => handleMaterialChange(idx, "cost", e.target.value)}
                              placeholder="150"
                              className="text-xs h-7 bg-background"
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  <Button
                    variant="outline"
                    className="w-full text-xs h-8 border-dashed"
                    onClick={handleAddMaterial}
                  >
                    <Plus className="h-3 w-3 mr-1" />
                    Add Manual Row
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}


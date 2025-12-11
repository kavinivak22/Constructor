"use client";

import { useActionState } from "react";
import { useFormStatus } from "react-dom";
import { getMaterialEstimation, type FormState } from "@/app/material-estimation/actions";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { Loader2, Wand2 } from "lucide-react";
import { readStreamableValue } from 'ai';
import { useEffect, useRef, useState } from "react";

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
          Generating...
        </>
      ) : (
        <>
          <Wand2 className="mr-2 h-4 w-4" />
          Generate Estimation
        </>
      )}
    </Button>
  );
}

export function EstimationForm() {
  const [state, formAction] = useActionState(getMaterialEstimation, initialState);
  const { toast } = useToast();
  const formRef = useRef<HTMLFormElement>(null);
  const [generation, setGeneration] = useState<string>('');

  useEffect(() => {
    if (state.message && state.message !== "Estimation successful.") {
      toast({
        title: "Error",
        description: state.message,
        variant: "destructive",
      });
    }
    if (state.message === "Estimation successful.") {
      formRef.current?.reset();
      setGeneration('');
      if (state.estimation?.estimation) {
        (async () => {
          let newContent = '';
          const streamableValue = state.estimation.estimation;
          // @ts-ignore
          for await (const delta of readStreamableValue(streamableValue)) {
            newContent = `${newContent}${delta}`;
            setGeneration(newContent);
          }
        })();
      }
    }
  }, [state, toast]);

  return (
    <div className="space-y-8">
      <form ref={formRef} action={formAction} className="space-y-6">
        <Card className="solid-card">
          <CardHeader>
            <CardTitle className="font-headline">Project Details</CardTitle>
            <CardDescription>
              Fill in the specifications for your project.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
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
          </CardContent>
          <CardFooter>
            <SubmitButton />
          </CardFooter>
        </Card>
      </form>

      {generation && (
        <Card className="solid-card">
          <CardHeader>
            <CardTitle className="font-headline flex items-center gap-2">
              <Wand2 className="text-primary" />
              Generated Material Estimation
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="prose prose-sm max-w-none dark:prose-invert whitespace-pre-wrap font-body">
              {generation}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

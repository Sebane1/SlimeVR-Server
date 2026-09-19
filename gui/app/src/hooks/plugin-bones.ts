import { atom } from 'jotai';
import { atomWithEffect } from 'jotai/utils';
import { useEffect, useCallback } from 'react';
import { GetPluginBonesResponseT, PluginBoneRegistrationT } from 'solarxr-protocol';

interface FetchPluginBonesResult {
  pluginBones: PluginBoneData[];
}

export interface PluginBoneData {
  id: string;
  name: string;
  parentBoneId: string;
  tabName?: string;
  position: Vector3;
  rotation: Quaternion;
  modelUrl?: string;
  assignedTrackerId?: number;
}

type Vector3 = [number, number, number];
type Quaternion = [number, number, number, number];

/**
 * Atom that fetches plugin bones from the server via RPC.
 */
export const fetchPluginBonesAtom = atomWithEffect<FetchPluginBonesResult>(async (get) => {
  // Fetch initial plugin bones
  try {
    const response = await window.rpc.call('GetPluginBonesResponse', {});
    
    if (response && typeof response === 'object' && response.registeredPluginBones) {
      const pluginBones: PluginBoneData[] = (response.registeredPluginBones as Array<PluginBoneRegistrationT>).map((bone, index) => ({
        id: bone.id ?? `plugin_bone_${index}`,
        name: bone.name ?? `Unnamed Bone ${index}`,
        parentBoneId: bone.parent_bone_id ?? '',
        tabName: undefined,
        position: [0, 0, 0], // Will be updated by InputProcessorExtension
        rotation: [0, 0, 0, 1], // Identity quaternion
        modelUrl: bone.model_url,
        assignedTrackerId: index < response.registeredPluginBones.length ? null : undefined,
      }));

      return { pluginBones };
    } else {
      return { pluginBones: [] };
    }
  } catch (error) {
    console.error('Failed to fetch plugin bones:', error);
    return { pluginBones: [] };
  }
});

/**
 * Hook for fetching and updating plugin bones.
 */
export function usePluginBones() {
  const [pluginBones, setPluginBones] = React.useState<PluginBoneData[]>([]);

  useEffect(() => {
    let mounted = true;
    
    async function fetchPluginBones(): Promise<void> {
      try {
        const response = await window.rpc.call('GetPluginBonesResponse', {});
        
        if (!mounted) return;
        if (response && typeof response === 'object' && response.registeredPluginBones) {
          const bones: PluginBoneData[] = (response.registeredPluginBones as Array<PluginBoneRegistrationT>).map((bone, index) => ({
            id: bone.id ?? `plugin_bone_${index}`,
            name: bone.name ?? `Unnamed Bone ${index}`,
            parentBoneId: bone.parent_bone_id ?? '',
            tabName: undefined,
            position: [0, 0, 0], // Will be updated by InputProcessorExtension
            rotation: [0, 0, 0, 1], // Identity quaternion
            modelUrl: bone.model_url,
          }));

          setPluginBones(bones);
        } else {
          setPluginBones([]);
        }
      } catch (error) {
        console.error('Failed to fetch plugin bones:', error);
      }
    }

    fetchPluginBones();

    return () => {
      mounted = false;
    };
  }, []);

  return pluginBones;
}

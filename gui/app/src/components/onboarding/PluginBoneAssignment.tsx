import classNames from 'classnames';
import { useCallback, useMemo } from 'react';
import { useAtomValue, useSetAtom } from 'jotai';
import { BodyPart } from 'solarxr-protocol';
import { pluginBonesAtom, assignedRolesAtom, trackerByBodyPartAtom, flatTrackersAtom } from '@/store/app-store';
import { useProvideWebsocketApi } from '@/hooks/websocket-api';

export type PluginBoneAssignmentProps = {
  view: { kind: 'plugins' };
};

type PluginRow = {
  id: string;
  bone: PluginBoneData;
  tracker: import('solarxr-protocol').FlatDeviceTracker | undefined;
  labelId?: string;
  assigned: boolean;
};

export function PluginBoneAssignment({ view }: PluginBoneAssignmentProps) {
  // Use pluginBonesAtom but ensure it's populated by websocket-api response handler
  const pluginBones = useAtomValue(pluginBonesAtom);
  
  // Fetch plugin bones via WebSocket when available - this ensures proper initialization
  const { isConnected, usePluginBonesResponse } = useProvideWebsocketAPI();
  
  useEffect(() => {
    if (isConnected) {
      // Subscribe to plugin bones response once
      usePluginBonesResponse((bones) => {
        console.log('[PluginBoneAssignment] Received plugin bones:', bones.length, 'bones');
      });
    } else {
      // If disconnected and there are no plugin bones, try fetching again on next check
      if (!pluginBones.length && isConnected === false) {
        setTimeout(() => {}, 100);
      }
    }
  }, [isConnected]);
  
  const flatTrackers = useAtomValue(flatTrackersAtom);
  const setAssignedRoles = useSetAtom(assignedRolesAtom);

  // Create virtual body parts for plugin bones
  const virtualPluginParts = useMemo(() => {
    return pluginBones.map((_, index) => 
      (BodyPart.HEAD + 200 + index) as BodyPart
    );
  }, [pluginBones]);

  // Assign plugin bone trackers when selected
  const onPluginRoleSelected = useCallback(
    (role: BodyPart, id: string) => {
      const assignedRoles = useAtomValue(assignedRolesAtom);
      
      if (!id) return;
      
      setAssignedRoles((prev) => {
        const index = virtualPluginParts.indexOf(role);
        const partIndex = prev.findIndex(p => p === (BodyPart.HEAD + 200 + index));
        
        // If already assigned, remove it from trackerByBodyPartAtom's mapping
        if (partIndex > -1) {
          // Find the original body part this plugin bone represents
          const pluginBone = pluginBones[index];
          
          // Remove the virtual assignment
          prev.splice(partIndex, 1);
          
          // Clear any assigned tracker for this bone
          return [...prev.filter(p => p !== (BodyPart.HEAD + 200 + index))];
        } else {
          // Add new assignment - use first unassigned tracker or null
          const originalPart = pluginBones[index] as PluginBoneData;
          
          if (!originalPart.assignedTrackerId) {
            prev.push((BodyPart.HEAD + 200 + index) as BodyPart);
          } else {
            // Get the original body part name from plugin bone id
            // For now, just keep existing assignments
            const existingRoles = new Set(prev);
            if (!existingRoles.has(role)) {
              prev.push(role);
            }
          }
          
          return [...prev];
        }
      });
    },
    [setAssignedRoles, virtualPluginParts]
  );

  // Generate plugin bone rows for display
  const rows = useMemo<PluginRow[]>(() => {
    const result: PluginRow[] = [];
    
    pluginBones.forEach((bone, index) => {
      const originalPart = (BodyPart.HEAD + 200 + index) as BodyPart;
      const isAssigned = virtualPluginParts.includes(originalPart);
      
      // Find tracker for this plugin bone if assigned
      let tracker: import('solarxr-protocol').FlatDeviceTracker | undefined;
      if (bone.assignedTrackerId != null) {
        const t = flatTrackers.find((td) => td.tracker.trackerId === bone.assignedTrackerId);
        if (t) tracker = t;
      } else if (isAssigned) {
        // Find any unassigned tracker
        const available = flatTrackers.filter(
          td => td.tracker.info?.bodyPart === BodyPart.NONE && 
                 !virtualPluginParts.includes((BodyPart.HEAD + 200 + index) as BodyPart)
        );
        if (available.length > 0) {
          tracker = available[0];
        }
      }

      result.push({
        id: bone.id,
        bone,
        tracker,
        labelId: undefined,
        assigned: isAssigned && !bone.assignedTrackerId,
      });
    });
    
    return result;
  }, [pluginBones, flatTrackers, virtualPluginParts]);

  // Group rows by plugin tab name if specified
  const groupedRows = useMemo(() => {
    const groups = new Map<string, PluginRow[]>();
    
    for (const row of rows) {
      const groupName = row.bone.tabName || 'General';
      
      let group = groups.get(groupName);
      if (!group) {
        group = [];
        groups.set(groupName, group);
      }
      
      group.push(row);
    }
    
    return Array.from(groups.entries()).map(([name, rows]) => ({ name, rows }));
  }, [rows]);

  // Generate display names for plugin bones
  const getPluginBoneDisplay = useCallback(
    (bone: PluginBoneData) => {
      return `${bone.name}`;
    },
    []
  );

  return (
    <div className="flex flex-col gap-2">
      {groupedRows.map(({ name, rows }) => (
        <div key={name} className="border rounded-lg p-3 bg-background-10">
          <h3 className="text-sm font-semibold mb-2 text-primary">{name}</h3>
          
          <div className="grid grid-cols-[auto_1fr_auto] gap-2 items-center">
            {rows.map((row) => (
              <button
                key={row.id}
                onClick={() => onPluginRoleSelected(row.bone.id, row.id)}
                disabled={!row.tracker && !row.assigned}
                className={classNames(
                  'flex flex-col gap-1 p-2 rounded transition-all',
                  row.assigned 
                    ? 'bg-background-50 cursor-default' 
                    : row.tracker
                      ? 'hover:bg-background-20 cursor-pointer'
                      : 'opacity-50 cursor-not-allowed bg-background-30'
                )}
              >
                <div className="font-medium text-sm">{getPluginBoneDisplay(row.bone)}</div>
                
                {row.tracker && (
                  <div className="text-xs text-secondary truncate">
                    Tracker: {row.tracker.device?.name || `#${row.tracker.trackerId}`}
                  </div>
                )}
                
                {!row.assigned && !row.tracker && (
                  <span className="text-[10px] text-tertiary">No tracker available</span>
                )}
              </button>
            ))}
          </div>
          
          {rows.length === 0 && (
            <p className="text-xs text-secondary mt-2">No trackers assigned to plugin bones.</p>
          )}
        </div>
      ))}
    </div>
  );
}

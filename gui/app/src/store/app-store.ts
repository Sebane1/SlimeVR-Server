import { atom } from 'jotai';
import { useSetAtom } from 'jotai/utils';
import { useAtomValue as _useAtomValue } from 'jotai';
import { useEffect } from 'react';
import {
  BodyPart,
  BoneT,
  DataFeedUpdateT,
  DeviceDataT,
  DeviceOrigin,
  DongleDataT,
  DongleStatus,
  GetPluginBonesResponse,
  TrackerStatus,
} from 'solarxr-protocol';
import { selectAtom } from 'jotai/utils';
import { isEqual } from '@react-hookz/deep-equal';

export interface FlatDeviceTracker {
  device?: DeviceDataT;
  tracker: TrackerDataT;
}

export interface PluginBoneData {
  /** ID for this bone - used as key in maps and for identification */
  id: string;
  
  /** Display name shown to users in skeleton preview */
  name: string;
  
  /** Parent bone reference (e.g., "HEAD", "SPINE") or null for independent bone */
  parentBoneId?: string;
  
  /** Optional tab grouping for organizing bones in UI */
  tabName?: string;
  
  /** URL to a 3D model file for this bone (optional GLB/GLTF) */
  modelUrl?: string;
  
  /** Assigned tracker ID if already assigned */
  assignedTrackerId?: number;
}

/** Get the next available virtual body part index starting from HEAD */
export function getNextPluginBoneIndex(): BodyPart {
  const pluginHead = BodyPart.HEAD + 200;
  return pluginHead as BodyPart;
}

export const ignoredTrackersAtom = atom(new Set<string>());

export const datafeedAtom = atom(new DataFeedUpdateT());

let pluginBonesSetter: ((bones: PluginBoneData[]) => void) | null = null;

export function setPluginBones(bones: PluginBoneData[]) {
  if (pluginBonesSetter) {
    pluginBonesSetter(bones);
  }
}

export const pluginBonesAtom = atom<PluginBoneData[]>([]);



export const bonesAtom = atom<BoneT[]>([]);

export const devicesAtom = selectAtom(
  datafeedAtom,
  (datafeed) => datafeed.devices,
  isEqual
);

export const serverGuardsAtom = selectAtom(
  datafeedAtom,
  (datafeed) => datafeed.serverGuards,
  isEqual
);

export const donglesAtom = selectAtom(
  datafeedAtom,
  (datafeed) => datafeed.dongles,
  isEqual
);

export type TrackerConnectionGroup = {
  key: string;
  assigned: FlatDeviceTracker[];
  unassigned: FlatDeviceTracker[];
} & (
  | {
      kind: 'dongle';
      dongleId: number;
      dongleName: string | null;
      status: DongleStatus;
    }
  | { kind: 'wifi' }
  | { kind: 'driver' }
);

export function groupTrackersByConnection(
  trackers: FlatDeviceTracker[],
  dongles: DongleDataT[],
  pluginBones: PluginBoneData[] = []
): TrackerConnectionGroup[] {
  const dongleByDeviceId = new Map<number, DongleDataT>(
    dongles.flatMap((dongle) => dongle.devicesIds.map((id) => [id, dongle]))
  );

  const dongleGroups = new Map<
    number,
    Extract<TrackerConnectionGroup, { kind: 'dongle' }>
  >();

  const wifiGroup: Extract<TrackerConnectionGroup, { kind: 'wifi' }> = {
    key: 'wifi',
    kind: 'wifi',
    assigned: [],
    unassigned: [],
  };

  const driverGroup: Extract<TrackerConnectionGroup, { kind: 'driver' }> = {
    key: 'driver',
    kind: 'driver',
    assigned: [],
    unassigned: [],
  };

  const pluginAssignedTrackerIds = new Set(
    pluginBones
      .map((b) => b.assignedTrackerId)
      .filter((id): id is number => id != null)
  );

  const getGroup = (flatTracker: FlatDeviceTracker): TrackerConnectionGroup => {
    if (flatTracker.tracker.origin == DeviceOrigin.DRIVER) {
      return driverGroup;
    }

    // 2. Dongle check
    const deviceId = flatTracker.device?.id;
    const dongle = deviceId != null ? dongleByDeviceId.get(deviceId) : undefined;

    if (!dongle) {
      return wifiGroup;
    }

    let dongleGroup = dongleGroups.get(dongle.id);
    if (!dongleGroup) {
      dongleGroup = {
        key: `dongle-${dongle.id}`,
        kind: 'dongle',
        dongleId: dongle.id,
        dongleName:
          dongle.customName?.toString() || dongle.displayName?.toString() || null,
        status: dongle.status,
        assigned: [],
        unassigned: [],
      };
      dongleGroups.set(dongle.id, dongleGroup);
    }

    return dongleGroup;
  };

  for (const flatTracker of trackers) {
    const group = getGroup(flatTracker);
    const isUnassigned =
      (flatTracker.tracker.info?.bodyPart === BodyPart.NONE ||
        flatTracker.tracker.info?.bodyPart == null) &&
      !pluginAssignedTrackerIds.has(flatTracker.tracker.trackerId);
    const targetList = isUnassigned ? group.unassigned : group.assigned;

    targetList.push(flatTracker);
  }

  const isNotEmpty = (group: TrackerConnectionGroup) =>
    group.assigned.length > 0 || group.unassigned.length > 0;

  const getTrackerCount = (group: TrackerConnectionGroup) =>
    group.assigned.length + group.unassigned.length;

  const activeGroups = [
    ...dongleGroups.values(),
    ...(isNotEmpty(wifiGroup) ? [wifiGroup] : []),
    ...(isNotEmpty(driverGroup) ? [driverGroup] : []),
  ];

  return activeGroups.sort((a, b) => {
    // Driver group always goes last
    if (a.kind === 'driver') return 1;
    if (b.kind === 'driver') return -1;

    // Otherwise sort descending by total tracker count
    return getTrackerCount(b) - getTrackerCount(a);
  });
}

export function groupTrackersByDevice(
  trackers: FlatDeviceTracker[]
): FlatDeviceTracker[][] {
  const order: number[] = [];
  const byDevice = new Map<number, FlatDeviceTracker[]>();
  trackers.forEach((td) => {
    const key = td.device?.id ?? td.tracker.trackerId;
    if (!byDevice.has(key)) {
      order.push(key);
      byDevice.set(key, []);
    }
    byDevice.get(key)!.push(td);
  });
  return order.map((key) => byDevice.get(key)!);
}

export function groupTrackerByBodyPart(
  trackers: FlatDeviceTracker[]
): Partial<Record<BodyPart, FlatDeviceTracker>> {
  const byPart: Partial<Record<BodyPart, FlatDeviceTracker>> = {};
  trackers.forEach((td) => {
    byPart[td.tracker.info?.bodyPart ?? BodyPart.NONE] = td;
  });
  return byPart;
}

export const flatTrackersAtom = atom((get) => {
  const devices = get(devicesAtom);

  return devices.flatMap<FlatDeviceTracker>((device) =>
    device.trackers.map((tracker) => ({ tracker, device }))
  );
});

export const assignedTrackersAtom = atom((get) => {
  const trackers = get(flatTrackersAtom);
  const pluginBones = get(pluginBonesAtom);
  const pluginAssignedTrackerIds = new Set(
    pluginBones
      .map((b) => b.assignedTrackerId)
      .filter((id): id is number => id != null)
  );
  return trackers.filter(
    ({ tracker }) =>
      (tracker.info?.bodyPart != null &&
        tracker.info?.bodyPart !== BodyPart.NONE) ||
      pluginAssignedTrackerIds.has(tracker.trackerId)
  );
});

export const trackerByBodyPartAtom = atom((get) => {
  const trackers = get(flatTrackersAtom);
  const pluginBones = get(pluginBonesAtom);
  const byPart: Partial<Record<BodyPart, FlatDeviceTracker>> = {};
  trackers.forEach((td) => {
    byPart[td.tracker.info?.bodyPart ?? BodyPart.NONE] = td;
  });
  pluginBones.forEach((bone, index) => {
    if (bone.assignedTrackerId != null) {
      const td = trackers.find(
        (t) => t.tracker.trackerId === bone.assignedTrackerId
      );
      if (td) {
        const vPart = (BodyPart.HEAD + 100 + index) as BodyPart;
        byPart[vPart] = td;
      }
    }
  });
  return byPart;
});

export function usePluginBones() {
  const pluginBones = _useAtomValue(pluginBonesAtom);
  
  // Fetch plugin bones when connected - this is called by WebSocket API handler
  useEffect(() => {
    console.log('[usePluginBones] Component mounted, checking connection...');
    
    // Get the setter function and call it if available
    // This will be populated by the websocket-api hook when a response arrives
    const setter = (bones: PluginBoneData[]) => {
      setPluginBones(bones);
    };
    
    return () => console.log('[usePluginBones] Unmounting');
  }, []);
  
  return pluginBones;
}

export const handleGetPluginBonesResponse = (bones: PluginBoneData[]) => {
  console.log('[app-store] handleGetPluginBonesResponse called with', bones.length, 'plugin bone(s)');
  
  setPluginBones(bones);
};

export const assignedRolesAtom = selectAtom(
  assignedTrackersAtom,
  (trackers) => trackers.map(({ tracker }) => tracker.info?.bodyPart ?? BodyPart.NONE),
  (a, b) => a.length === b.length && a.every((part, i) => part === b[i])
);

export const unassignedTrackersAtom = atom((get) => {
  const trackers = get(flatTrackersAtom);
  return trackers.filter(({ tracker }) => tracker.info?.bodyPart === BodyPart.NONE);
});

export const connectedTrackersAtom = atom((get) => {
  const trackers = get(flatTrackersAtom);
  return trackers.filter(
    ({ tracker }) => tracker.status !== TrackerStatus.DISCONNECTED
  );
});

export const connectedIMUTrackersAtom = atom((get) => {
  const trackers = get(connectedTrackersAtom);
  return trackers.filter(({ tracker }) => tracker.info?.isImu);
});

export const trackerFromIdAtom = ({
  trackerNum,
  deviceId,
}: {
  trackerNum: string | number | undefined;
  deviceId: string | number | undefined;
}) =>
  selectAtom(
    atom((get) =>
      get(flatTrackersAtom).find(
        ({ tracker }) =>
          trackerNum &&
          deviceId &&
          tracker?.trackerId == trackerNum &&
          tracker?.deviceId == deviceId
      )
    ),
    (a) => a,
    isEqual
  );

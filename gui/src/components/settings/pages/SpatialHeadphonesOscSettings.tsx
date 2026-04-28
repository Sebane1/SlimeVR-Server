import { Localized, useLocalization } from '@fluent/react';
import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import {
  ChangeSettingsRequestT,
  RpcMessage,
  SettingsRequestT,
  SettingsResponseT,
  OSCSettingsT,
  SpatialHeadphonesOscSettingsT
} from 'solarxr-protocol';
import { useWebsocketAPI } from '@/hooks/websocket-api';
import { CheckBox } from '@/components/commons/Checkbox';
import { RouterIcon } from '@/components/commons/icon/RouterIcon';
import { Input } from '@/components/commons/Input';
import { Typography } from '@/components/commons/Typography';
import {
  SettingsPageLayout,
  SettingsPagePaneLayout,
} from '@/components/settings/SettingsPageLayout';
import { yupResolver } from '@hookform/resolvers/yup';
import { object } from 'yup';
import {
  OSCSettings,
  useSpatialHeadphonesOscSettingsValidator,
} from '@/hooks/osc-setting-validator';

interface SpatialHeadphonesOscSettingsForm {
  spatialHeadphones: {
    oscSettings: OSCSettings;
  };
}

const defaultValues = {
  spatialHeadphones: {
    oscSettings: {
      enabled: false,
      portOut: 7001,
      address: '127.0.0.1',
    },
  },
};

export function SpatialHeadphonesOscSettings() {
  const { l10n } = useLocalization();
  const { sendRPCPacket, useRPCPacket } = useWebsocketAPI();
  const { oscValidator } = useSpatialHeadphonesOscSettingsValidator();

  const { reset, control, watch, handleSubmit } =
    useForm<SpatialHeadphonesOscSettingsForm>({
      defaultValues,
      reValidateMode: 'onChange',
      mode: 'onChange',
      resolver: yupResolver(
        object({
          spatialHeadphones: object({
            oscSettings: oscValidator,
          }),
        }) as any,
      ),
    });

  const onSubmit = (values: SpatialHeadphonesOscSettingsForm) => {
    const settings = new ChangeSettingsRequestT();

    const osc = new SpatialHeadphonesOscSettingsT();
    Object.assign(osc, values.spatialHeadphones.oscSettings);
    settings.spatialHeadphonesOsc = osc;
    sendRPCPacket(RpcMessage.ChangeSettingsRequest, settings);
  };

  useEffect(() => {
    const subscription = watch(() => handleSubmit(onSubmit as any)());
    return () => subscription.unsubscribe();
  }, []);

  useEffect(() => {
    sendRPCPacket(RpcMessage.SettingsRequest, new SettingsRequestT());
  }, []);

  useRPCPacket(RpcMessage.SettingsResponse, (settings: SettingsResponseT) => {
    const formData: SpatialHeadphonesOscSettingsForm = defaultValues;
    if (settings.spatialHeadphonesOsc) {
        formData.spatialHeadphones.oscSettings.enabled =
        settings.spatialHeadphonesOsc.enabled;
        formData.spatialHeadphones.oscSettings.portOut =
        settings.spatialHeadphonesOsc.portOut;
        if (settings.spatialHeadphonesOsc.address) {
        formData.spatialHeadphones.oscSettings.address =
            settings.spatialHeadphonesOsc.address.toString();
        }
    }
    reset(formData);
  });

  return (
    <SettingsPageLayout>
      <form className="flex flex-col gap-2 w-full">
        <SettingsPagePaneLayout icon={<RouterIcon />} id="spatialHeadphones">
          <>
            <Typography variant="main-title">
              {l10n.getString('settings-osc-spatial-headphones')}
            </Typography>
            <div className="flex flex-col pt-2 pb-4">
              <>
                {l10n
                  .getString('settings-osc-spatial-headphones-description')
                  .split('\n')
                  .map((line, i) => (
                    <Typography key={i}>{line}</Typography>
                  ))}
              </>
            </div>
            <Typography variant="section-title">
              {l10n.getString('settings-osc-spatial-headphones-enable')}
            </Typography>
            <div className="flex flex-col pb-2">
              <Typography>
                {l10n.getString(
                  'settings-osc-spatial-headphones-enable-description',
                )}
              </Typography>
            </div>
            <div className="grid grid-cols-2 gap-3 pb-5">
              <CheckBox
                variant="toggle"
                outlined
                control={control}
                name="spatialHeadphones.oscSettings.enabled"
                label={l10n.getString(
                  'settings-osc-spatial-headphones-enable-label',
                )}
              />
            </div>

            <Typography variant="section-title">
              {l10n.getString('settings-osc-spatial-headphones-network')}
            </Typography>
            <div className="flex flex-col pb-2">
              <Typography>
                {l10n.getString(
                  'settings-osc-spatial-headphones-network-description',
                )}
              </Typography>
            </div>
            <div className="grid grid-cols-2 gap-3 pb-5">
              <Localized
                id="settings-osc-spatial-headphones-network-port_out"
                attrs={{ placeholder: true, label: true }}
              >
                <Input
                  type="number"
                  control={control}
                  name="spatialHeadphones.oscSettings.portOut"
                  placeholder="7001"
                  label=""
                />
              </Localized>
            </div>
            <Typography variant="section-title">
              {l10n.getString(
                'settings-osc-spatial-headphones-network-address',
              )}
            </Typography>
            <div className="flex flex-col pb-2">
              <Typography>
                {l10n.getString(
                  'settings-osc-spatial-headphones-network-address-description',
                )}
              </Typography>
            </div>
            <div className="grid gap-3 pb-5">
              <Input
                type="text"
                control={control}
                name="spatialHeadphones.oscSettings.address"
                placeholder={l10n.getString(
                  'settings-osc-spatial-headphones-network-address-placeholder',
                )}
                label=""
              />
            </div>
          </>
        </SettingsPagePaneLayout>
      </form>
    </SettingsPageLayout>
  );
}

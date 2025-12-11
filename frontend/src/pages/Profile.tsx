import React, { useEffect, useState } from "react";
import api from "@/services/api/client";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";


type MeResponse = {
  userId?: string;
  id?: string;
  username?: string;
  fullName?: string;
  roles?: string[] | string;
};

const Profile: React.FC = () => {
  const { user: authUser } = useAuth() as any;

  const [loading, setLoading] = useState<boolean>(true);
  const [savingProfile, setSavingProfile] = useState<boolean>(false);
  const [changingPassword, setChangingPassword] = useState<boolean>(false);

  const [profile, setProfile] = useState<MeResponse | null>(null);

  // edit fields
  const [username, setUsername] = useState<string>("");
  const [fullName, setFullName] = useState<string>("");

  // password fields
  const [oldPassword, setOldPassword] = useState<string>("");
  const [newPassword, setNewPassword] = useState<string>("");
  const [confirmPassword, setConfirmPassword] = useState<string>("");

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const res = await api.get("/users/me");
      const data: MeResponse = res.data ?? {};
      setProfile(data);
      setUsername(data.username ?? "");
      setFullName(data.fullName ?? data.fullName ?? "");
    } catch (err: any) {
      console.error("Failed to fetch profile:", err);
      toast.error(err?.response?.data?.message || "Failed to load profile");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Save both fullName and username (username is treated as email here)
  const handleSaveProfile = async (e?: React.FormEvent) => {
    e?.preventDefault();

    // basic validation
    if (!username || username.trim().length === 0) {
      toast.error("Username (email) cannot be empty");
      return;
    }
    // simple email format check (you can remove or replace with a stricter check)
    const emailLike = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailLike.test(username.trim())) {
      toast.error("Username must be a valid email address");
      return;
    }

    setSavingProfile(true);
    try {
      const payload = {
        username: username.trim(),
        fullName: fullName?.trim() || null,
      };
      await api.put("/users/me", payload);
      toast.success("Profile updated");
      await fetchProfile();
    } catch (err: any) {
      console.error("Save profile failed:", err);
      toast.error(err?.response?.data?.message || "Failed to save profile");
    } finally {
      setSavingProfile(false);
    }
  };

  const handleChangePassword = async (e?: React.FormEvent) => {
    e?.preventDefault();

    if (!oldPassword || !newPassword) {
      toast.error("Please fill both current and new password");
      return;
    }
    if (newPassword.length < 8) {
      toast.error("New password should be at least 8 characters");
      return;
    }
    if (newPassword !== confirmPassword) {
      toast.error("New password and confirmation do not match");
      return;
    }

    setChangingPassword(true);
    try {
      const payload = {
        oldPassword,
        newPassword,
      };
      await api.put("/users/me/password", payload);
      toast.success("Password changed");
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: any) {
      console.error("Change password failed:", err);
      toast.error(err?.response?.data?.message || "Failed to change password");
    } finally {
      setChangingPassword(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[240px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-semibold">My profile</h2>
        <div className="text-sm text-muted-foreground">
          {authUser ? `Logged in as ${authUser.username || authUser.email}` : "Not logged in"}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Account</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSaveProfile} className="space-y-4">
              <div>
                <Label htmlFor="fullName">Full name</Label>
                <Input id="fullName" value={fullName} onChange={(e) => setFullName(e.target.value)} />
              </div>

              <div>
                <Label htmlFor="username">Username (email)</Label>
                <Input id="username" value={username} onChange={(e) => setUsername(e.target.value)} />
              </div>

              <div className="flex items-center gap-3 mt-2">
                <Button type="submit" disabled={savingProfile}>
                  {savingProfile ? "Saving…" : "Save profile"}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Change password</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleChangePassword} className="space-y-4">
              <div>
                <Label htmlFor="oldPassword">Current password</Label>
                <Input id="oldPassword" type="password" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} />
              </div>

              <div>
                <Label htmlFor="newPassword">New password</Label>
                <Input id="newPassword" type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
              </div>

              <div>
                <Label htmlFor="confirmPassword">Confirm new password</Label>
                <Input id="confirmPassword" type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
              </div>

              <div className="flex items-center gap-3 mt-2">
                <Button type="submit" disabled={changingPassword}>
                  {changingPassword ? "Changing…" : "Change password"}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Profile;